package com.xyz.gumall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xyz.common.utils.HttpUtils;
import com.xyz.common.utils.R;
import com.xyz.common.vo.MemberRespVo;
import com.xyz.gumall.auth.feign.MemberFeignService;
import com.xyz.gumall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class Oauth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/github/success")
    public String authorize(@RequestParam("code") String code, HttpSession session) throws Exception {

        Map<String, String> query = new HashMap<>();
        query.put("client_id", "yourclient_id ");
        query.put("client_secret", "yourclient_secret");
        query.put("redirect_uri", "http://auth.gumall.com/oauth2.0/github/success");
        query.put("code", code);

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        HttpResponse response = HttpUtils.doPost("https://github.com", "/login/oauth/access_token", "post", headers, query, new HashMap<String, String>());

        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            R oauthlogin = memberFeignService.oauthlogin(socialUser);

            if (oauthlogin.getCode() == 0) {
                MemberRespVo data = oauthlogin.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登入成功:{}" ,data.toString());
                session.setAttribute("loginUser", data);
                return "redirect:http://gumall.com";
            } else {
                return "redirect:http://auth.gumall.com/login.html";
            }
        }else {
            return "redirect:http://auth.gumall.com/login.html";
        }
    }
}
