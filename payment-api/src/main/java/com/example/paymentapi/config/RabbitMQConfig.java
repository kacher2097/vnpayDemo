package com.example.paymentapi.config;

import com.example.paymentapi.model.RabbitMQProperties;
import com.example.paymentapi.util.PropertiesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Configuration
public class RabbitMQConfig {
    private static final String FILE_CONFIG = "\\config\\rabbitmq-config.properties";

    private static RabbitMQConfig instance;
    public static RabbitMQConfig getInstance(){
        if(instance == null){
            instance = new RabbitMQConfig();
        }
        return instance;
    }

    public RabbitMQProperties readConfigFile(){
        log.info("Begin read config rabbitmq file ");
        Properties properties = PropertiesUtils.getInstance();
        InputStream inputStream = null;
        RabbitMQProperties rabbitMQProperties = new RabbitMQProperties();
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            // get property by name
            rabbitMQProperties.setUserName(properties.getProperty("username"));
            rabbitMQProperties.setPassword(properties.getProperty("password"));
            rabbitMQProperties.setHost(properties.getProperty("host"));
            rabbitMQProperties.setQueue(properties.getProperty("queue"));
            rabbitMQProperties.setPort(Integer.parseInt(properties.getProperty("port")));
            rabbitMQProperties.setMaxChannel(Integer.parseInt(properties.getProperty("max_channel")));
            rabbitMQProperties.setTimeOut(Integer.parseInt(properties.getProperty("timeout")));
            log.info("Get properties success");
        } catch (IOException e) {
            log.error("Read file config fail {}", e);
            e.printStackTrace();
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    log.info("Close input stream");
                    inputStream.close();
                }
            } catch (IOException e) {
               log.error("Close input stream fail");
            }
        }
        return rabbitMQProperties;
    }

}
