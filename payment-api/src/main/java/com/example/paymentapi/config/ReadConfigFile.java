package com.example.paymentapi.config;

import com.example.paymentapi.model.RabbitMQProperties;
import com.example.paymentapi.util.PropertiesUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReadConfigFile {

    private static PropertiesUtils propertiesUtils;
    private static final String FILE_CONFIG_RABBITMQ = "\\config\\rabbitmq-config.properties";
    private static final String FILE_CONFIG_REDIS = "\\config\\redis-config.properties";
    private static final String FILE_DESCRIPTION_ERROR = "\\config\\error-description.properties";

    private static ReadConfigFile instance;
//    public static ReadConfigFile getInstance(String pathConfig){
//        if(instance == null){
//            instance = new ReadConfigFile();
//            readConfigFile(pathConfig);
//        }
//        return instance;
//    }

//    public static RabbitMQProperties readConfigFile(String pathConfig, Object object){
//        Properties properties = propertiesUtils.getInstance();
//        InputStream inputStream = null;
//        RabbitMQProperties rabbitMQProperties = new RabbitMQProperties();
//        try {
//            String currentDir = System.getProperty("user.dir");
//            inputStream = new FileInputStream(currentDir + pathConfig);
//
//            // load properties from file
//            properties.load(inputStream);
//
//            // get property by name
//            rabbitMQProperties.setUserName(properties.getProperty("username"));
//            rabbitMQProperties.setPassword(properties.getProperty("password"));
//            rabbitMQProperties.setHost(properties.getProperty("host"));
//            rabbitMQProperties.setQueue(properties.getProperty("queue"));
//            rabbitMQProperties.setPort(Integer.parseInt(properties.getProperty("port")));
//            rabbitMQProperties.setMaxChannel(Integer.parseInt(properties.getProperty("max_channel")));
//            rabbitMQProperties.setTimeOut(Integer.parseInt(properties.getProperty("timeout")));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // close objects
//            try {
//                if (inputStream != null) {
//                    inputStream.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return rabbitMQProperties;
//    }

}
