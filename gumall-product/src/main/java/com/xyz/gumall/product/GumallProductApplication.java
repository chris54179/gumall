package com.xyz.gumall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.xyz.gumall.product.feign")
@EnableDiscoveryClient
@MapperScan("com.xyz.gumall.product.dao")
@SpringBootApplication
public class GumallProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(GumallProductApplication.class, args);
    }
}
