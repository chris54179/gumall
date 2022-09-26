package com.xyz.gumall.order.web;

import com.xyz.common.exception.NoStockException;
import com.xyz.gumall.order.service.OrderService;
import com.xyz.gumall.order.vo.OrderConfirmVo;
import com.xyz.gumall.order.vo.OrderSubmitVo;
import com.xyz.gumall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model, HttpServletRequest request) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", confirmVo);
        return "confirm";
    }
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        try{
            SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
            // 根据vo中定义的状态码来验证
            if (responseVo.getCode() == 0 ) { // 订单创建成功
                // 下单成功返回到支付页
                model.addAttribute("submitOrderResp", responseVo);
                return "pay";
            } else { // 下单失败
                String msg = "下单失败;";
                switch (responseVo.getCode()) {
                    case 1:
                        msg += "防重令牌校验失败";
                        break;
                    case 2:
                        msg += "商品价格发生变化，請確認後再次提交";
                        break;
                    case 3:
                        msg += "庫存鎖定失敗，商品庫存不足";
                        break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                // 重新回到订单确认页面
                return "redirect:http://order.gumall.com/toTrade";
            }
        }catch (Exception e){
            if (e instanceof NoStockException){
//                String msg = "下单失败，商品无库存";
//                redirectAttributes.addFlashAttribute("msg", msg);
                String message = ((NoStockException) e).getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.gumall.com/toTrade";
        }
    }
}
