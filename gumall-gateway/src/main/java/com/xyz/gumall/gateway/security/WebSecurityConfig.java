package com.xyz.gumall.gateway.security;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Flux;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;

//@EnableWebFluxSecurity
public class WebSecurityConfig {
    @Autowired
    CustomHttpBasicServerAuthenticationEntryPoint customServerAuthenticationEntryPoint;
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        RedirectServerAuthenticationEntryPoint loginPoint = new RedirectServerAuthenticationEntryPoint("/server-a/account/index");
        http.authorizeExchange().pathMatchers("/server-a/easyui/**","/server-a/js/**","/server-a/account/index","/server-a/account/login",
                "/",
                "/login.html",
                "/login"
        ).permitAll()
                .and().cors()
                .and().formLogin().loginPage("/server-a/account/authen").authenticationEntryPoint(loginPoint)
                .authenticationSuccessHandler((webFilterExchange,authentication)->{//认证成功之后返回给客户端的信息
                    JSONObject result = new JSONObject();
                    result.put("code", 0);
                    return webFilterExchange.getExchange().getResponse().writeWith(Flux.create(sink -> {
                        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
                        try {
                            DataBuffer dataBuffer= nettyDataBufferFactory.wrap(result.toJSONString().getBytes("utf8"));
                            sink.next(dataBuffer);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        sink.complete();
                    }));
                })
                .and().authorizeExchange().pathMatchers("/server-a/account/main").access(new MyReactiveAuthorizationManager("Manager", "Dev"))
                .and().authorizeExchange().anyExchange().authenticated()
                .and().exceptionHandling().authenticationEntryPoint(customServerAuthenticationEntryPoint)
                .and().csrf().disable();
        SecurityWebFilterChain chain = http.build();
        Iterator<WebFilter>  weIterable = chain.getWebFilters().toIterable().iterator();
        while(weIterable.hasNext()) {
            WebFilter f = weIterable.next();
            if(f instanceof AuthenticationWebFilter) {
                AuthenticationWebFilter webFilter = (AuthenticationWebFilter) f;
                //将自定义的AuthenticationConverter添加到过滤器中
                webFilter.setServerAuthenticationConverter(new MyAuthenticationConverter());
            }
        }
        return chain;
    }
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return new ReactiveAuthenticationManagerAdapter((authentication)->{
            if(authentication instanceof MyAccountAuthentication) {
                MyAccountAuthentication gmAccountAuthentication = (MyAccountAuthentication) authentication;
                if(gmAccountAuthentication.getPrincipal() != null) {
                    authentication.setAuthenticated(true);
                    return authentication;
                } else {
                    return authentication;
                }
            } else {
                return authentication;
            }
        });
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource (new PathPatternParser());
        CorsConfiguration corsConfig = new CorsConfiguration ();

        // 允许所有请求方法
        corsConfig.addAllowedMethod ("*");
        // 允许所有域，当请求头
        corsConfig.addAllowedOrigin ("*");
        // 允许全部请求头
        corsConfig.addAllowedHeader ("*");
        // 允许携带 Authorization 头
        corsConfig.setAllowCredentials(true);
        // 允许全部请求路径
        source.registerCorsConfiguration ("/**", corsConfig);

        return source;
    }
}
