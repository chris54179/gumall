package com.xyz.gumall.product.web;

import com.xyz.gumall.product.service.SkuInfoService;
import com.xyz.gumall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.ExecutionException;

@Controller
public class itemController {

    @Autowired
    SkuInfoService skuInfoService;

    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) throws ExecutionException, InterruptedException {
        System.out.println("準備查詢"+skuId+"詳情");
        SkuItemVo vo =skuInfoService.item(skuId);
        model.addAttribute("item", vo);
        return "item";
    }
}
