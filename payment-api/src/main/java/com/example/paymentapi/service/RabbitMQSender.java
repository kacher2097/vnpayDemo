package com.example.paymentapi.service;

import com.example.paymentapi.config.RabbitMQConfig;
import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.service.impl.PaymentServiceImpl;
import com.example.paymentapi.util.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

@Service
public class RabbitMQSender {
    private static Logger log = LoggerFactory.getLogger(RabbitMQSender.class);
    private final RabbitMQConfig rabbitMQConfig;

//    private String rpcQueue = "queue.rpc";

//    public void send(PaymentRequest paymentRequest){
//        rabbitTemplate.convertAndSend(exchange, routingKey, paymentRequest);
//    }

    public RabbitMQSender(RabbitMQConfig rabbitMQConfig) throws IOException, TimeoutException {
        this.rabbitMQConfig = rabbitMQConfig;
    }

    public String call(PaymentRequest paymentRequest) throws IOException, InterruptedException, TimeoutException {
        Channel channel = rabbitMQConfig.getChannel();
        String rpcQueue = rabbitMQConfig.readConfigFile().getQueue();
        final String corrId = UUID.randomUUID().toString();
        StringBuilder stringBuilder = new StringBuilder();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        log.info("correlationId {} ", corrId);
        String message = Convert.convertObjToString(paymentRequest);
        channel.basicPublish("", rpcQueue, props, message.getBytes("UTF-8"));
        log.info("send to: {} ", props);
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    response.offer(new String(delivery.getBody(), "UTF-8"));
//                    stringBuilder.append(new String(delivery.getBody(), "UTF-8"));
                }

            } catch (RuntimeException e) {
                throw new RequestException("010", "RabbitMQ fail");
            }
        };

        String ctag = channel.basicConsume(replyQueueName, false, deliverCallback, consumerTag -> {
        });

        String result = response.take();
        channel.basicCancel(ctag);
        return result;
    }

}
