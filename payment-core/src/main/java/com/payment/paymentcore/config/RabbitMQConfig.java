package com.payment.paymentcore.config;


import com.payment.paymentcore.model.RabbitMQModel;
import com.payment.paymentcore.util.ErrorCode;
import com.payment.paymentcore.util.PaymentException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RabbitMQConfig {
    private static final String FILE_CONFIG = "\\config\\rabbitmq-config.properties";

    public RabbitMQModel readConfigFile() throws PaymentException {
        Properties properties = new Properties();
        InputStream inputStream = null;
        RabbitMQModel rabbitMQModel = new RabbitMQModel();
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            // get property by name
            rabbitMQModel.setUserName(properties.getProperty("username"));
            rabbitMQModel.setPassword(properties.getProperty("password"));
            rabbitMQModel.setHost(properties.getProperty("host"));
            rabbitMQModel.setQueue(properties.getProperty("queue"));
            rabbitMQModel.setPort(Integer.parseInt(properties.getProperty("port")));
            rabbitMQModel.setMaxChannel(Integer.parseInt(properties.getProperty("max_channel")));
            rabbitMQModel.setTimeOut(Integer.parseInt(properties.getProperty("timeout")));

        } catch (IOException e) {
            throw new PaymentException(ErrorCode.READ_CONFIG_RABBITMQ_FAIL);
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rabbitMQModel;
    }
}
