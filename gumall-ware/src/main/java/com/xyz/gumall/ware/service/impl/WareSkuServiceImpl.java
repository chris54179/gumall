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
        //??????????????????????????????
        wareSkuDao.unlockStock(skuId, wareId, num);
        //????????????????????????????????????
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);//???????????????
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
            //TODO ????????????sku???????????????????????????????????????????????????
            //1?????????catch??????
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
        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
//        taskEntity.setCreateTime(new Date());
        orderTaskService.save(taskEntity);

        // 1.?????????????????????????????????????????????
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //??????????????????????????????????????????
            List<Long> wareIds = wareSkuDao.listWareIdsHasStock(item.getSkuId());
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        // 2.????????????
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
//            boolean lock = true;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            //????????????????????????????????????????????????
            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            } else {
                for (Long wareId : wareIds) {
                    //????????????1????????????0
                    Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                    if (count == 1) {
                        //????????????????????????????????????
                        skuStocked = true;
                        //TODO ??????MQ??????????????????
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
                        //?????????????????????????????????????????????
//                        WareOrderTaskDetailEntity detailEntity = WareOrderTaskDetailEntity.builder()
//                                .skuId(skuId)
//                                .skuName("")
//                                .skuNum(lockVo.getNum())
//                                .taskId(taskEntity.getId())
//                                .wareId(wareId)
//                                .lockStatus(1).build();
//                        wareOrderTaskDetailService.save(detailEntity);
//                        //???????????????????????????????????????
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
        // 3.??????????????????
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
//            System.out.println("??????????????????:");
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if (byId != null) {
            //??????
            Long id = to.getId();
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();//????????????????????????????????????
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                //????????????
                OrderVo data = r.getData("order", new TypeReference<OrderVo>() {
                });
                //??????????????????||???????????????????????? ????????????
                if (data == null || data.getStatus() == 4) {
                    //???????????????????????????????????????????????????????????????????????????????????????
                    if (byId.getLockStatus() == 1){
                        unlockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //????????????????????????????????????????????????????????????????????????
                throw new RuntimeException("??????????????????");
            }
        } else {
            //????????????
        }
    }

    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        //???????????????????????????????????????????????????
        String orderSn = orderTo.getOrderSn();
        WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();
        //?????????????????????????????????????????????????????????????????????
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
