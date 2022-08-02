package com.payment.paymentcore;

import com.payment.paymentcore.service.RabbitMQService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Main {
    private static final RabbitMQService rabbitMQService;

    static {
        try {
            rabbitMQService = RabbitMQService.getInstance();
        } catch (IOException | TimeoutException e) {
            log.error("Error RabbitMQService {}", e);
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){
        rabbitMQService.consumeAndPublishMessage();
    }
}





