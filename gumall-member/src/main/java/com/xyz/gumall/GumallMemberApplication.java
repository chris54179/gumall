package com.xyz.gumall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.xyz.gumall.member.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class GumallMemberApplication {

	public static void main(String[] args) {
		SpringApplication.run(GumallMemberApplication.class, args);
	}

}
