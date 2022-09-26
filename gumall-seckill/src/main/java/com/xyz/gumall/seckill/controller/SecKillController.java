package com.xyz.gumall.seckill.controller;

import com.xyz.common.utils.R;
import com.xyz.gumall.seckill.service.SecKillService;
import com.xyz.gumall.seckill.to.SeckillSkuRedisTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
public class SecKillController {

    @Autowired
    private SecKillService secKillService;

    /**
     * 当前时间可以参与秒杀的商品信息
     * @return
     */
    @GetMapping(value = "/currentSeckillSkus")
    @ResponseBody
    public R getCurrentSeckillSkus() {
        log.info("currentSeckillSkus正在執行..");
        //获取到当前可以参加秒杀商品的信息
        List<SeckillSkuRedisTo> vos = secKillService.getCurrentSeckillSkus();
        return R.ok().setData(vos);
    }

    @ResponseBody
    @GetMapping(value = "/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SeckillSkuRedisTo to = secKillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);
    }


    @GetMapping("/kill")
    public String seckill(@RequestParam("killId") String killId,
                       @RequestParam("key")String key,
                       @RequestParam("num")Integer num,
                       Model model) {
        String orderSn = secKillService.kill(killId, key, num);
        model.addAttribute("orderSn", orderSn);
        return "success";
    }

}
