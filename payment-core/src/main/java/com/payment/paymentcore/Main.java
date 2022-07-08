package com.payment.paymentcore;

import com.payment.paymentcore.config.RabbitMQConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final RabbitMQConfig rabbitMqConfig;

    static {
        try {
            rabbitMqConfig = RabbitMQConfig.getInstance();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        rabbitMqConfig.sendAndReceiveMessage();
    }
}





