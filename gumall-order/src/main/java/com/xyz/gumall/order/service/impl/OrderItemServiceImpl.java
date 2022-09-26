package com.xyz.gumall.order.service.impl;

import com.xyz.gumall.order.entity.OrderEntity;
import com.xyz.gumall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xyz.common.utils.PageUtils;
import com.xyz.common.utils.Query;

import com.xyz.gumall.order.dao.OrderItemDao;
import com.xyz.gumall.order.entity.OrderItemEntity;
import com.xyz.gumall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

//    @RabbitListener(queues = {"hello-java-queue"})
    @RabbitHandler
    public void recieveMessage(Message message,
                               OrderReturnReasonEntity content,
                               Channel channel) throws InterruptedException {
        System.out.println("接收到消息.."+content);
        byte[] body = message.getBody();
        MessageProperties properties = message.getMessageProperties();
//        System.out.println("接收到消息.."+message+" =>類型:"+message.getClass());
//        Thread.sleep(3000);
        System.out.println("消息處理完成=>"+content.getName());
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.out.println("deliveryTag="+deliveryTag);
        try {
            if (deliveryTag % 2 == 0) {
                channel.basicAck(deliveryTag, false);
                System.out.println("簽收了..."+deliveryTag);
            }else {
                channel.basicNack(deliveryTag,false,false);
//                channel.basicReject(deliveryTag,false);
                System.out.println("沒有簽收..."+deliveryTag);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitHandler
    public void recieveMessage(OrderEntity content) throws InterruptedException {
        System.out.println("接收到消息.."+content);

    }
}
