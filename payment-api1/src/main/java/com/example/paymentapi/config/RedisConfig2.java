package com.example.paymentapi.config;

import com.example.paymentapi.exception.RequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;

@Configuration
public class RedisConfig2 {
    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    public Jedis getRedis(){
        try {
            Jedis jedis = new Jedis(redisHost, redisPort);
            return jedis;
        } catch (JedisConnectionException e){
            throw new RequestException("78", e.getMessage());
        }

    }

}
