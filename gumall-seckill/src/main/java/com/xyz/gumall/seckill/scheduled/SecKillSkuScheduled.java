package com.xyz.gumall.seckill.scheduled;

import com.xyz.gumall.seckill.service.SecKillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SecKillSkuScheduled {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SecKillService secKillService;

    //秒杀商品上架功能的锁
    private final String upload_lock = "seckill:upload:lock";

    /**
     * 定时任务
     * 每天三点上架最近三天的秒杀商品
     */
    @Async
//    @Scheduled(cron = "* * * ? * 3")
    @Scheduled(cron = "*/3 * * * * ?")
//    @Scheduled(cron = "* 33 17 * * ?")
    //TODO 冪等性處理
    public void uploadSeckillSkuLatest3Days() {
        log.info("上架秒殺..");
        //为避免分布式情况下多服务同时上架的情况，使用分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            secKillService.uploadSeckillSkuLatest3Days();
        }finally {
            lock.unlock();
        }
    }
}
