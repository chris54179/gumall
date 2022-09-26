package com.xyz.gumall.gateway.security;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerFormLoginAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class MyAuthenticationConverter extends ServerFormLoginAuthenticationConverter{
    private static Logger logger = LoggerFactory.getLogger(MyAuthenticationConverter.class);
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
       //从session中获取登陆用户信息
//        String value = exchange.getSession().block().getAttribute("AccountInfo");
        Object accountInfo = exchange.getSession().toProcessor().block().getAttribute("AccountInfo");
        String value = accountInfo.toString();

        if(value == null) {
           logger.error("用户认证信息为空,返回获取认证信息失败");
           return Mono.empty();
       } else {
           List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
           //获取权限信息
           List<String> roels = JSON.parseObject(value).getJSONArray("roles").toJavaList(String.class);
               roels.forEach(role->{
                   //这里必须添加前缀，参考：AuthorityReactiveAuthorizationManager.hasRole(role)
                   SimpleGrantedAuthority auth = new SimpleGrantedAuthority("ROLE_" + role);
                   simpleGrantedAuthorities.add(auth);
               });
            //添加用户信息到spring security之中。
           MyAccountAuthentication myAccountAuthentication = new MyAccountAuthentication(null, value, simpleGrantedAuthorities);
           return Mono.just(myAccountAuthentication);
       }
    }
}
