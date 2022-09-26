package com.xyz.gumall.order;

import com.xyz.gumall.order.entity.OrderEntity;
import com.xyz.gumall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GumallOrderApplicationTests {

	@Autowired
	AmqpAdmin amqpAdmin;
	@Autowired
	RabbitTemplate rabbitTemplate;

	@Test
	public void sendMessageTest(){
		for (int i = 0; i < 10; i++) {
			if (i%2 == 0){
				OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
				reasonEntity.setId(1L);
				reasonEntity.setCreateTime(new Date());
				reasonEntity.setName("haha-"+i);
				rabbitTemplate.convertAndSend("hello-java-exchange","hello.java", reasonEntity,new CorrelationData(UUID.randomUUID().toString()));
			}else {
				OrderEntity entity = new OrderEntity();
				entity.setOrderSn(UUID.randomUUID().toString());
				rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",entity,new CorrelationData(UUID.randomUUID().toString()));
			}
			log.info("send ok {}");
		}

//		String msg = "hello world2";
//		rabbitTemplate.convertAndSend("hello-java-exchange","hello.java", msg);
//		log.info("send ok {}", msg);
	}

	@Test
	public void createExchange() {
		DirectExchange directExchange = new DirectExchange("hello-java-exchange",true,false);
		amqpAdmin.declareExchange(directExchange);
		log.info("Exchange {} is created success", directExchange.getName());
	}

	@Test
	public void createQueue(){
		Queue queue = new Queue("hello-java-queue",true,false,false);
		amqpAdmin.declareQueue(queue);
		log.info("Queue {} is created success", "hello-java-queue");
	}

	@Test
	public void createBinding(){
		Binding binding = new Binding("hello-java-queue",
				Binding.DestinationType.QUEUE,
				"hello-java-exchange",
				"hello.java",null);
		amqpAdmin.declareBinding(binding);
		log.info("Binding {} is created success", "hello-java-binding");


	}
}
