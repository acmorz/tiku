package com.fsh.tiku.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@Slf4j
public class MyRabbitConfig {

    @Lazy
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 用于进行类型的转换
     * @return
     */
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 需要先在application中设置为simple
     */
    public void initRabbitTemplate(){
        // Broker设置确认回调，成功抵达就回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *
             * @param correlationData 当前消息的唯一关联数据（这个是消息的唯一id）
             * @param ack 消息是否成功收到
             * @param cause 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                log.info("确认消息：" + correlationData + "。是否成功：ack" + "。若失败，则原因为：" + cause);
            }
        });

        //设置消息抵达队列的确认回调，当投递到队列失败的时候会进行回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {

            @Override
            public void returnedMessage(ReturnedMessage returned) {
                /**
                 * 	private final Message message; 消息投递失败的详细信息
                 *
                 * 	private final int replyCode; 回复的状态码
                 *
                 * 	private final String replyText; 回复的文本内容
                 *
                 * 	private final String exchange; 这个消息发送给了哪个交换机
                 *
                 * 	private final String routingKey; 这个消息用哪个路由键
                 */
                log.info("交换机" + returned.getExchange() + "传递给队列失败", "详细信息为：" + returned.getMessage() + "状态码为：" + returned.getReplyCode());
            }
        });
    }

}
