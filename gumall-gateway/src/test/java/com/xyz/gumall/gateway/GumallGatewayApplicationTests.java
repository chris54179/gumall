package com.xyz.gumall.gateway;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@Slf4j
@RunWith(SpringRunner.class)
public class GumallGatewayApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void Test() {
		log.trace("trace 成功了");
		log.debug("debug 成功了");
		log.info("info 成功了");
		log.warn("warn 成功了");
		log.error("error 成功了");
//		for(int i=0;i<5;i++) {
//			log.debug("{},测试日志{}",i, "看看可能写入logstash");
//			log.info("{},测试日志{}",i, "看看可能写入logstash");
//		}
	}
}
