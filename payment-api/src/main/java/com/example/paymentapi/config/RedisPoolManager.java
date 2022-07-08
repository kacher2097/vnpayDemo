package com.example.paymentapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RedisPoolManager implements ClientConfigUpdateListener {
    private static Logger logger = Logger.getLogger(RedisPoolManager.class.getName());

    ConcurrentMap<String, JedisPool> pools = new ConcurrentHashMap<>();
    public static final String REDIS_KEY = "redis";
    @Autowired
    public RedisPoolManager(ClientConfigHandler configHandler)
    {
        configHandler.addListener(this);
    }

    private void add(String client,String host,int maxTotal,int maxIdle)
    {
        logger.info("Adding Redis pool for "+client+" at "+host+" maxTotal "+maxTotal+" maxIdle "+maxIdle);
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal); // maximum active connections
        poolConfig.setMaxIdle(maxIdle);  // maximum idle connections

        JedisPool pool = new JedisPool(poolConfig, host);
        JedisPool existing = pools.get(client);
        pools.put(client, pool);
        if (existing != null)
        {
            logger.warn("Attempting to close previous pool for "+client);
            existing.destroy();
        }
    }

    private void remove(String client)
    {
        logger.info("Removing pool for "+client);
        JedisPool pool = pools.remove(client);
        if (pool != null)
        {
            pool.destroy();
        }
    }

    public JedisPool get(String client)
    {
        return pools.get(client);
    }

    @Override
    public void configUpdated(String client, String configKey,
                              String configValue) {
        if (configKey.equals(REDIS_KEY)){
            logger.info("Received new redis config for "+ client+": "+ configValue);
            try {
                ObjectMapper mapper = new ObjectMapper();
                RedisConfig config = mapper.readValue(configValue, RedisConfig.class);
                add(client,config.host,config.maxTotal,config.maxIdle);
                logger.info("Successfully added new redis config for "+client);
            } catch (IOException | BeansException e) {
                logger.error("Couldn't update redis for client " +client, e);
            }
        }

    }

    public static class RedisConfig
    {
        public String host;
        public int maxTotal = 10;
        public int maxIdle = 2;
    }

    @Override
    public void configRemoved(String client, String configKey) {
        if (configKey.equals(REDIS_KEY)){
            remove(client);
        }
    }

}
