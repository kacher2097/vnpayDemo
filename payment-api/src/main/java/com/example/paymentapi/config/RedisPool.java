package com.example.paymentapi.config;

import com.example.paymentapi.exception.PaymentException;
import com.example.paymentapi.model.RedisPropertiesObject;
import com.example.paymentapi.util.ErrorCode;
import com.example.paymentapi.util.PropertiesUtils;
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
    private static final RedisPropertiesObject redisPropertiesObject = new RedisPropertiesObject();
    private static final JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

    public RedisPool() {
        readConfigRedisFile();
        initPool();
        init();
    }

    private static void initPool() {
        jedisPoolConfig.setMinIdle(redisPropertiesObject.getMinIdle());
        jedisPoolConfig.setMaxTotal(redisPropertiesObject.getMaxTotal());
        jedisPoolConfig.setMaxIdle(redisPropertiesObject.getMaxIdle());
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setBlockWhenExhausted(true);
        jedisPoolConfig.setMaxWaitMillis(redisPropertiesObject.getMaxWait());
    }

    private static void init() {
        pool = new JedisPool(jedisPoolConfig, redisPropertiesObject.getRedisHost(),
                redisPropertiesObject.getRedisPort(), 1000 * 2);
    }

    public static void readConfigRedisFile() {
        log.info("Begin read redis config file");
        final String FILE_CONFIG = "./config/redis-config.properties";
        Properties properties = PropertiesUtils.getInstance();
        InputStream inputStream = null;
        try {
//            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream( FILE_CONFIG);

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
            log.info("End read redis config file success");
        } catch (IOException e) {
            log.error("IOException read file redis config file error {}", e);
            throw new PaymentException(ErrorCode.READ_CONFIG_REDIS_FAIL);
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    log.info("Close input stream of read redis config file ");
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("IOException read file redis config file error {}", e);
            }
        }
//        return redisPropertiesObject;
    }

    public Jedis getJedis() {
        try {
            log.info("Get resource from Redis pool");
            return pool.getResource();
        } catch (Exception e) {
            log.error("Get resource Redis fail {}", e);
            throw new PaymentException(ErrorCode.CONNECT_REDIS_FAIL);
        }

    }

}
