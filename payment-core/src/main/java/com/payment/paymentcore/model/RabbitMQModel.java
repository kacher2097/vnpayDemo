package com.payment.paymentcore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RabbitMQModel {
    private String userName;
    private String password;
    private String queue;
    private String host;
    private int port;
    private int maxChannel;
    private int timeOut;

}
