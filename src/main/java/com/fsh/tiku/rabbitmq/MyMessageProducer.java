package com.fsh.tiku.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MyMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;


    public void sendMessage(String exchange, String routingKey, Object message) {
        /**
         * 消息发送到的交换机的名称
         * 路由键决定发送到哪个队列
         * 发送内容
         */
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
