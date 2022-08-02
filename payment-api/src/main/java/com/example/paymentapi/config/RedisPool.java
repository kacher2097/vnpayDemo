package com.example.paymentapi.config;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.RedisPropertiesObject;
import com.example.paymentapi.util.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Configuration
public class RedisPool {

    private static JedisPool pool;

    static {
        initPool();
    }

    private static void initPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        RedisPropertiesObject redisPropertiesObject = readConfigFile();
        config.setMinIdle(redisPropertiesObject.getMinIdle());
        config.setMaxTotal(redisPropertiesObject.getMaxTotal());
        config.setMaxIdle(redisPropertiesObject.getMaxIdle());
        config.setTestOnBorrow(true);
        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(redisPropertiesObject.getMaxWait());
        pool = new JedisPool(config, redisPropertiesObject.getRedisHost(),
                redisPropertiesObject.getRedisPort(), 1000 * 2);
    }

    public static RedisPropertiesObject readConfigFile(){
        final String FILE_CONFIG = "\\config\\redis-config.properties";
        Properties properties = new Properties();
        InputStream inputStream = null;
        RedisPropertiesObject redisPropertiesObject = new RedisPropertiesObject();
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            // get property by name
            redisPropertiesObject.setMinIdle(Integer.parseInt(properties.getProperty("min_idle")));
            redisPropertiesObject.setMaxIdle(Integer.parseInt(properties.getProperty("max_idle")));
            redisPropertiesObject.setMaxTotal(Integer.parseInt(properties.getProperty("max_total")));
            redisPropertiesObject.setMaxWait(Integer.parseInt(properties.getProperty("max_wait")));
            redisPropertiesObject.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("test_on_borrow")));
            redisPropertiesObject.setRedisHost(properties.getProperty("redis_host"));
            redisPropertiesObject.setRedisPort(Integer.parseInt(properties.getProperty("redis_port")));

        } catch (IOException e) {
            log.error("IOException read file redis config file error {}", e);
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("IOException read file redis config file error {}", e);
            }
        }
        return redisPropertiesObject;
    }

    public Jedis getJedis() {
        try {
            return pool.getResource();
        } catch (Exception e){
            log.info("Get resource Redis fail");
            throw new RequestException(ErrorCode.CONNECT_REDIS_FAIL);
        }

    }
    
}
