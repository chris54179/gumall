package com.xyz.gumall.seckill.service;

import com.xyz.gumall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface SecKillService {
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
