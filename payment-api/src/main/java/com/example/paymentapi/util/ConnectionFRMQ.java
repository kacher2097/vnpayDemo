package com.example.paymentapi.util;

import com.rabbitmq.client.ConnectionFactory;

public class ConnectionFRMQ {
    private static ConnectionFactory instance;
    public static ConnectionFactory getInstance(){
        if(instance == null){
            instance = new ConnectionFactory();
        }

        return instance;
    }
}
