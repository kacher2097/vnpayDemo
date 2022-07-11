package com.example.paymentapi.config;

import com.example.paymentapi.model.RedisPropertiesObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RedisPool {

    private static JedisPool pool;

    private static void initPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        RedisPropertiesObject redisPropertiesObject = readConfigFile();
        config.setMaxTotal(redisPropertiesObject.getMaxTotal());
        config.setMaxIdle(redisPropertiesObject.getMaxIdle());
        config.setTestOnBorrow(true);
        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(redisPropertiesObject.getMaxWait());

        pool = new JedisPool(config, redisPropertiesObject.getRedisHost(), redisPropertiesObject.getRedisPort(), 1000 * 2);
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
            redisPropertiesObject.setMaxIdle(Integer.parseInt(properties.getProperty("max_idle")));
            redisPropertiesObject.setMaxTotal(Integer.parseInt(properties.getProperty("max_total")));
            redisPropertiesObject.setMaxWait(Integer.parseInt(properties.getProperty("max_wait")));
            redisPropertiesObject.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("test_on_borrow")));
            redisPropertiesObject.setRedisHost(properties.getProperty("redis_host"));
            redisPropertiesObject.setRedisPort(Integer.parseInt(properties.getProperty("redis_port")));

        } catch (IOException e) {
            e.printStackTrace();
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
        return redisPropertiesObject;
    }


    static {
        initPool();
    }

    public Jedis getJedis() {
        return pool.getResource();
    }

    public static void jedisPoolClose(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}
