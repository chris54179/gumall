package com.xyz.gumall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.xyz.gumall.order.config.AlipayTemplate;
import com.xyz.gumall.order.service.OrderService;
import com.xyz.gumall.order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 异步接收支付宝成功回调
 */
@RestController
public class OrderPayedListener {

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    @GetMapping("/payed/get")
    public String handleAlipayed() {
        System.out.println("get");
        return "s";
    }

    @PostMapping("/payed/notify")
    public String handleAlipayed(PayAsyncVo vo, HttpServletRequest request) throws AlipayApiException, UnsupportedEncodingException {
//        Map<String, String[]> map = request.getParameterMap();
//        for (String key : map.keySet()) {
//            String value = request.getParameter(key);
//            System.out.println("參數名:"+key+"==>參數值:"+value);
//        }

//        System.out.println("支付寶通知到位..數據="+map);
        /**
         * 重要一步验签名
         * 防止别人通过postman给我们发送一个请求，告诉我们请求成功，为了防止这种效果通过验签
         */
        Map<String,String> params = new HashMap<String,String>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
//            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }
        // 支付宝验签 防止恶意提交
        boolean signVerified = AlipaySignature.rsaCheckV1(params
                , alipayTemplate.getAlipay_public_key()
                , alipayTemplate.getCharset()
                , alipayTemplate.getSign_type());
        if (signVerified) {
            System.out.println("簽名驗證成功...");
            String result = orderService.handlePayResult(vo);
            return result;
        } else {
            System.out.println("簽名驗證失敗...");
            return "error";
        }
    }

    @GetMapping("/payed/test")
    public String test() {
        return "test";
    }
}
