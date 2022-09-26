package com.xyz.gumall.product.feign;

import com.xyz.common.utils.R;
import com.xyz.gumall.product.feign.fallback.SeckillFeignServiceFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "gumall-seckill", fallback = SeckillFeignServiceFallBack.class)
public interface SeckillFeignService {
    @GetMapping(value = "/sku/seckill/{skuId}")
    R getSkuSeckillInfo(@PathVariable("skuId") Long skuId);
}
