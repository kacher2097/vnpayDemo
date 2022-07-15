package com.example.paymentapi.service;

import com.example.paymentapi.config.RabbitMQConfig;
import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.util.Convert;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class RabbitMQSender {
    private final RabbitMQConfig rabbitMQConfig;

    public RabbitMQSender(RabbitMQConfig rabbitMQConfig) {
        this.rabbitMQConfig = rabbitMQConfig;
    }

    public String call(PaymentRequest paymentRequest) throws IOException, InterruptedException, TimeoutException {
        Channel channel = rabbitMQConfig.getChannel();
        String rpcQueue = rabbitMQConfig.readConfigFile().getQueue();

        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        log.info("correlationId {} ", corrId);
        String message = Convert.convertObjToString(paymentRequest);

        log.info("Send message to queue {} with data: {}", rpcQueue, message);
        channel.basicPublish("", rpcQueue, props, message.getBytes(StandardCharsets.UTF_8));
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    log.info("get body from server: ", delivery.getBody());
                    response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
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
