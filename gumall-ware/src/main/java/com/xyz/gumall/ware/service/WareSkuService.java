package com.xyz.gumall.ware.service;

import com.xyz.common.to.mq.OrderTo;
import com.xyz.common.to.mq.StockLockedTo;
import com.xyz.gumall.ware.vo.SkuHasStockVo;
import com.xyz.gumall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 *
 *
 *
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo lockVo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);
}

