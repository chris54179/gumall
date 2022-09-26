package com.xyz.gumall.ware;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableFeignClients
@EnableTransactionManagement
@MapperScan("com.xyz.gumall.ware.dao")
@EnableDiscoveryClient
@SpringBootApplication
public class GumallWareApplication {

	public static void main(String[] args) {
		SpringApplication.run(GumallWareApplication.class, args);
	}

}
