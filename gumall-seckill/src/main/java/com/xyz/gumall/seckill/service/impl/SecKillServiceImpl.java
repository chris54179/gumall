package com.xyz.gumall.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xyz.common.to.mq.SeckillOrderTo;
import com.xyz.common.utils.R;
import com.xyz.common.vo.MemberRespVo;
import com.xyz.gumall.seckill.feign.CouponFeignService;
import com.xyz.gumall.seckill.feign.ProductFeignService;
import com.xyz.gumall.seckill.interceptor.LoginUserInterceptor;
import com.xyz.gumall.seckill.service.SecKillService;
import com.xyz.gumall.seckill.to.SeckillSkuRedisTo;
import com.xyz.gumall.seckill.vo.SeckillSessionsWithSkus;
import com.xyz.gumall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SecKillServiceImpl implements SecKillService {

    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    RedissonClient redissonClient;

    //K: SESSION_CACHE_PREFIX + startTime + "_" + endTime
    //V: sessionId+"-"+skuId的List
    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";

    //K: 固定值SECKILL_CHARE_PREFIX
    //V: hash，k为sessionId+"-"+skuId，v为对应的商品信息SeckillSkuRedisTo
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    //K: SKU_STOCK_SEMAPHORE+商品随机码
    //V: 秒杀的库存件数
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";    //+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        R session = couponFeignService.getlates3DaySession();
        if (session.getCode() == 0) {
            //上架商品
            List<SeckillSessionsWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //在redis中分别保存秒杀场次信息和场次对应的秒杀商品信息
            saveSessionInfos(sessionData);
            saveSessionSkuInfos(sessionData);
        }
    }

    public List<SeckillSkuRedisTo> blockHandler(BlockException e) {
        log.error("getCurrentSeckillSkusResource被限流了..");
        return null;
        //自定義返回數據
//        SeckillSkuRedisTo seckillSkuRedisTo = new SeckillSkuRedisTo();
//        seckillSkuRedisTo.setRandomCode("test");
//        List<SeckillSkuRedisTo> list = new ArrayList<>();
//        list.add(seckillSkuRedisTo);
//        return list;
    }

    @SentinelResource(value = "getCurrentSeckillSkusResource",blockHandler = "blockHandler")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1.確定當前時間屬於哪個秒殺場次
        long time = new Date().getTime();

        try(Entry entry = SphU.entry("seckillSkus")){
            Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
            for (String key : keys) {
                String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                long start = Long.parseLong(s[0]);
                long end = Long.parseLong(s[1]);
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            System.out.println(new Date(time));
//            System.out.println(new Date(start));
//            System.out.println(new Date(end));
                //当前秒杀活动处于有效期内
                if (time >= start && time <= end) {
                    //2.獲取這個秒殺場次需要的所有商品信息
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                    List<String> list = hashOps.multiGet(range);
                    if (list != null) {
                        List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                            SeckillSkuRedisTo redis = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
//                        redis.setRandomCode(null); //當前秒殺開始就需要隨機碼
                            return redis;
                        }).collect(Collectors.toList());
                        return collect;
                    }
                    break;
                }
            }
        }catch (BlockException e){
            log.error("資源被限流,{}",e.getMessage());
        }

        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                String regx = "\\d_" + skuId;
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    //当前商品参与秒杀活动
                    if (skuRedisTo!=null){
                        long current = new Date().getTime();
                        //当前活动在有效期，暴露商品随机码返回
                        if (current >= skuRedisTo.getStartTime() && current <= skuRedisTo.getEndTime()) {

                        } else {
                            skuRedisTo.setRandomCode(null);
                        }
                        return skuRedisTo;
                    }
                }
            }
        }
        return null;
    }

    //RSemaphore
    @Override
    public String kill(String killId, String key, Integer num) {
        long s1 = System.currentTimeMillis();
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
        //1.獲取當前秒殺商品的詳細信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String s = hashOps.get(killId);
        String orderSn = null;
        if (StringUtils.isEmpty(s)){
            return null;
        } else {
            SeckillSkuRedisTo redis = JSON.parseObject(s, SeckillSkuRedisTo.class);
            //1. 验证时效
            Long startTime = redis.getStartTime();
            Long endTime = redis.getEndTime();
            long time = new Date().getTime();

            long ttl = endTime - time;
            if (time >= startTime && time <= endTime) {
                //2. 验证商品和商品随机码是否对应
                String randomCode = redis.getRandomCode();
                String skuId = redis.getPromotionSessionId() + "_" + redis.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    //3. 验证購物數量是否合理
                    if (num <= redis.getSeckillLimit()) {
                        //4.驗證這個人是否已經購買過。秒殺成功就去佔位
                        String redisKey = respVo.getId() + "_" + skuId;
                        //自動過期
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean) {
                            //佔位成功說明從來沒有買過
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                                boolean b = semaphore.tryAcquire(num);
                                //秒殺成功
                                if (b) {
                                    //快速下單。發送MQ消息
                                    String timeId = IdWorker.getTimeId();
                                    SeckillOrderTo orderTo = new SeckillOrderTo();
                                    orderTo.setOrderSn(timeId);
                                    orderTo.setMemberId(respVo.getId());
                                    orderTo.setNum(num);
                                    orderTo.setPromotionSessionId(redis.getPromotionSessionId());
                                    orderTo.setSkuId(redis.getSkuId());
                                    orderTo.setSeckillPrice(redis.getSeckillPrice());
                                    rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order", orderTo);
                                    long s2 = System.currentTimeMillis();
                                    log.info("耗時.."+(s2-s1));
                                    return timeId;
                                }
                                return null;
                        }else {
                            //說明已經買過了
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    //RLock
//    @Override
//    public String kill(String killId, String key, Integer num) {
//        long s1 = System.currentTimeMillis();
//        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
//        //1.獲取當前秒殺商品的詳細信息
//        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
//        String s = hashOps.get(killId);
//        String orderSn = null;
//        if (StringUtils.isEmpty(s)) {
//            return null;
//        } else {
//            final String lockKey = "seckill:skus:" + String.valueOf(killId) + "_" + "-RedissonLock";
//            SeckillSkuRedisTo redis = JSON.parseObject(s, SeckillSkuRedisTo.class);
//            String randomCode = redis.getRandomCode();
//            String skuId = redis.getPromotionSessionId() + "_" + redis.getSkuId();
//            if (randomCode.equals(key) && killId.equals(skuId)) {
//                // 加锁
//                RLock lock = redissonClient.getLock(lockKey);
//                try {
//                    // 第一个参数为 5， 尝试获取锁的的最大等待时间为5s
//                    // 第二个参数为 10，  上锁成功后10s后锁自动失效
//                    // 尝试获取锁（可重入锁,不会造成死锁）
//                    boolean lockFlag = lock.tryLock(5, 10, TimeUnit.SECONDS);
//                    if (lockFlag) {
//                        //3. 验证購物數量是否合理
//                        if (num > redis.getSeckillLimit()) {
//                            throw new Exception("超出購買上限");
//                        } else {
//                            // 做幂等性处理
//                            log.info("用户：" + respVo.getId() + "---已抢到商品：" + killId + "，不可以重新领取");
//                            Integer wareCount = Integer.valueOf(redisTemplate.opsForValue().get(SKU_STOCK_SEMAPHORE + randomCode));
//                            if (wareCount > 0) {
//                                redisTemplate.opsForValue().set(SKU_STOCK_SEMAPHORE + randomCode, String.valueOf(wareCount - num));
//                            } else {
//                                System.err.println("用户：" + respVo.getId() + "---未抢到商品：" + killId);
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.出现了错误");
//                } finally {
//                    // 解锁
//                    lock.unlock();
//                }
//                System.out.println("用户：" + respVo.getId() + "---抢到商品：" + killId);
//                //快速下單。發送MQ消息
//                String timeId = IdWorker.getTimeId();
//                SeckillOrderTo orderTo = new SeckillOrderTo();
//                orderTo.setOrderSn(timeId);
//                orderTo.setMemberId(respVo.getId());
//                orderTo.setNum(num);
//                orderTo.setPromotionSessionId(redis.getPromotionSessionId());
//                orderTo.setSkuId(redis.getSkuId());
//                orderTo.setSeckillPrice(redis.getSeckillPrice());
//                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
//                long s2 = System.currentTimeMillis();
//                log.info("耗時.." + (s2 - s1));
//                return timeId;
//            }
//        }
//        return null;
//    }

    //Redis + Lua
//        @Override
//        public String kill(String killId, String key, Integer num) {
//            long s1 = System.currentTimeMillis();
//            MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
//            //1.獲取當前秒殺商品的詳細信息
//            BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
//            String s = hashOps.get(killId);
//            String orderSn = null;
//            if (StringUtils.isEmpty(s)){
//                return null;
//            } else {
//                SeckillSkuRedisTo redis = JSON.parseObject(s, SeckillSkuRedisTo.class);
//                String randomCode = redis.getRandomCode();
//                String skuId = redis.getPromotionSessionId() + "_" + redis.getSkuId();
//                if (randomCode.equals(key) && killId.equals(skuId)) {
//                    // 加锁
//                    try {
//                            //3. 验证購物數量是否合理
//                            if (num > redis.getSeckillLimit()) {
//                                throw new Exception("超出購買上限");
//                            } else {
//                                // 做幂等性处理
//                                log.info("用户：" + respVo.getId() + "---已抢到商品：" + killId + "，不可以重新领取");
//                                Integer wareCount = Integer.valueOf(redisTemplate.opsForValue().get(SKU_STOCK_SEMAPHORE + randomCode));
//                                    // lua脚本执行对象
////                                    RScript script = redissonClient.getScript();
//                                RScript script = redissonClient.getScript(LongCodec.INSTANCE);
//                                List<Object> keys=new ArrayList<>();
//                                    keys.add(SKU_STOCK_SEMAPHORE + randomCode);
//
//                                    final String LOCK_STOCK_LUA=
//                                            "if tonumber(redis.call('get',KEYS[1])) >= tonumber(ARGV[1]) then " +
//                                                "redis.call('decrby',KEYS[1],ARGV[1]); " +
//                                                "   return 1;" +
//                                            "end; " +
//                                            "return 0;";
//
//                                    // 执行脚本
//                                Object result = script.eval(RScript.Mode.READ_WRITE, LOCK_STOCK_LUA, RScript.ReturnType.VALUE,keys , num);
////                                    Long result = script.eval(RScript.Mode.READ_WRITE, LOCK_STOCK_LUA, RScript.ReturnType.INTEGER,keys, num);
//                                    System.out.println("执行lua脚本后返回参数 "+result);
//                                    if (result.equals(1L)) {
//                                        System.out.println("用户：" + respVo.getId() + "---抢到商品：" + killId);
//                                        //快速下單。發送MQ消息
//                                        String timeId = IdWorker.getTimeId();
//                                        SeckillOrderTo orderTo = new SeckillOrderTo();
//                                        orderTo.setOrderSn(timeId);
//                                        orderTo.setMemberId(respVo.getId());
//                                        orderTo.setNum(num);
//                                        orderTo.setPromotionSessionId(redis.getPromotionSessionId());
//                                        orderTo.setSkuId(redis.getSkuId());
//                                        orderTo.setSeckillPrice(redis.getSeckillPrice());
//                                        rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order", orderTo);
//                                        long s2 = System.currentTimeMillis();
//                                        log.info("耗時.."+(s2-s1));
//                                        return timeId;
//                                    } else {
//                                        System.err.println("用户：" + respVo.getId() + "---未抢到商品：" + killId);
//                                    }
//                            }
//                    } catch (Exception e) {
//                        System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.出现了错误");
//                    }
//                }
//            }
//        return null;
//    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null) {
            sessions.stream().forEach(session->{
                long startTime = session.getStartTime().getTime();
                long endTime = session.getEndTime().getTime();

                String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
                Boolean hasKey = redisTemplate.hasKey(key);

                //緩存活動信息
                if (!hasKey) {
                    //当前活动信息未保存过
                    List<String> collect = session.getRelationSkus().stream()
                            .map(item -> item.getPromotionSessionId()+"_"+item.getSkuId().toString())
                            .collect(Collectors.toList());
                    redisTemplate.opsForList().leftPushAll(key, collect);
                }
            });
        }
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null) {
            sessions.stream().forEach(session -> {
                BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

                session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                    //4.隨機碼
                    String token = UUID.randomUUID().toString().replace("-", "");

                    if (!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString())) {
                        //緩存商品
                        SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                        //1.sku的基本數據
                        R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                        if (skuInfo.getCode() == 0) {
                            SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            redisTo.setSkuInfo(info);
                        }
                        //2.sku的秒殺信息
                        BeanUtils.copyProperties(seckillSkuVo, redisTo);
                        //3.設置上當前商品的秒殺時間信息
                        redisTo.setStartTime(session.getStartTime().getTime());
                        redisTo.setEndTime(session.getEndTime().getTime());

                        redisTo.setRandomCode(token);
                        String jsonString = JSON.toJSONString(redisTo);
                        ops.put(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString(), jsonString);
                        //5. 使用库存作为Redisson信号量限制库存
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                        //商品可以秒殺的數量作為信號量
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                    }
                });
            });
        }
    }

}
