package com.example.paymentapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisPropertiesObject {
    private int maxTotal;
    private int minIdle;
    private int maxIdle;
    private int maxWait;
    private boolean testOnBorrow;
    private String redisHost;
    private int redisPort;

}
