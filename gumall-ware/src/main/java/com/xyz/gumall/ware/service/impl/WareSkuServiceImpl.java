package com.xyz.gumall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.xyz.common.exception.NoStockException;
import com.xyz.common.to.mq.OrderTo;
import com.xyz.common.to.mq.StockDetailTo;
import com.xyz.common.to.mq.StockLockedTo;
import com.xyz.common.utils.PageUtils;
import com.xyz.common.utils.Query;
import com.xyz.common.utils.R;
import com.xyz.gumall.ware.dao.WareSkuDao;
import com.xyz.gumall.ware.entity.WareOrderTaskDetailEntity;
import com.xyz.gumall.ware.entity.WareOrderTaskEntity;
import com.xyz.gumall.ware.entity.WareSkuEntity;
import com.xyz.gumall.ware.feign.OrderFeignService;
import com.xyz.gumall.ware.feign.ProductFeignService;
import com.xyz.gumall.ware.service.WareOrderTaskDetailService;
import com.xyz.gumall.ware.service.WareOrderTaskService;
import com.xyz.gumall.ware.service.WareSkuService;
import com.xyz.gumall.ware.vo.OrderItemVo;
import com.xyz.gumall.ware.vo.OrderVo;
import com.xyz.gumall.ware.vo.SkuHasStockVo;
import com.xyz.gumall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    WareOrderTaskService orderTaskService;
    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;
    @Autowired
    OrderFeignService orderFeignService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    private void unlockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        //数据库中解锁库存数据
        wareSkuDao.unlockStock(skuId, wareId, num);
        //更新库存工作单详情的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);//變為已解鎖
        orderTaskDetailService.updateById(entity);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (entities == null || entities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //TODO 远程查询sku的名字，如果失败，整个事务无需回滚
            //1、自己catch异常
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");

                if (info.getCode() == 0) {
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            wareSkuDao.insert(skuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            Long count = this.baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //因为可能出现订单回滚后，库存锁定不回滚的情况，但订单已经回滚，得不到库存锁定信息，因此要有库存工作单
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
//        taskEntity.setCreateTime(new Date());
        orderTaskService.save(taskEntity);

        // 1.找到每個商品在哪個倉庫都有庫存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //找出所有库存大于商品数的仓库
            List<Long> wareIds = wareSkuDao.listWareIdsHasStock(item.getSkuId());
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        // 2.鎖定庫存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
//            boolean lock = true;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            //如果没有满足条件的仓库，抛出异常
            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            } else {
                for (Long wareId : wareIds) {
                    //成功返回1，否則為0
                    Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                    if (count == 1) {
                        //锁定成功，保存工作单详情
                        skuStocked = true;
                        //TODO 告訴MQ庫存鎖定成功
                        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1);
                        orderTaskDetailService.save(entity);
                        StockLockedTo lockedTo = new StockLockedTo();
                        lockedTo.setId(taskEntity.getId());
                        StockDetailTo stockDetailTo = new StockDetailTo();
                        BeanUtils.copyProperties(entity, stockDetailTo);
                        lockedTo.setDetail(stockDetailTo);
                        rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                        break;
                    } else {
                        //當前倉庫鎖失敗，重試下一個倉庫
//                        WareOrderTaskDetailEntity detailEntity = WareOrderTaskDetailEntity.builder()
//                                .skuId(skuId)
//                                .skuName("")
//                                .skuNum(lockVo.getNum())
//                                .taskId(taskEntity.getId())
//                                .wareId(wareId)
//                                .lockStatus(1).build();
//                        wareOrderTaskDetailService.save(detailEntity);
//                        //发送库存锁定消息至延迟队列
//                        StockLockedTo lockedTo = new StockLockedTo();
//                        lockedTo.setId(taskEntity.getId());
//                        StockDetailTo detailTo = new StockDetailTo();
//                        BeanUtils.copyProperties(detailEntity,detailTo);
//                        lockedTo.setDetailTo(detailTo);
//                        rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",lockedTo);
//
//                        lock = true;
//                        break;
                    }
                }
            }
            if (skuStocked == false) {
                throw new NoStockException(skuId);
            }
        }
        // 3.全部鎖定成功
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
//            System.out.println("收到解鎖庫存:");
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if (byId != null) {
            //解鎖
            Long id = to.getId();
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();//根據訂單號查詢訂單的狀態
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                //返回成功
                OrderVo data = r.getData("order", new TypeReference<OrderVo>() {
                });
                //没有这个订单||订单状态已经取消 解锁库存
                if (data == null || data.getStatus() == 4) {
                    //为保证幂等性，只有当工作单详情处于被锁定的情况下才进行解锁
                    if (byId.getLockStatus() == 1){
                        unlockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //消息拒絕以後重新放到隊列裡面，讓別人繼續消費解鎖
                throw new RuntimeException("遠程服務失敗");
            }
        } else {
            //無須解鎖
        }
    }

    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        //为防止重复解锁，需要重新查询工作单
        String orderSn = orderTo.getOrderSn();
        WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();
        //查询出当前订单相关的且处于锁定状态的工作单详情
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService
                .list(new QueryWrapper<WareOrderTaskDetailEntity>()
                        .eq("task_id", task.getId())
                        .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            unlockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}
