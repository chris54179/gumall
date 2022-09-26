package com.xyz.gumall.order.feign;

import com.xyz.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gumall-product")
public interface ProductFeignService {
    @RequestMapping("/product/spuinfo/skuId/{id}")
    R getSpuInfoBySkuId(@PathVariable("id") Long skuId);

//    @RequestMapping("product/skuinfo/info/{skuId}")
//    R info(@PathVariable("skuId") Long skuId);
}
