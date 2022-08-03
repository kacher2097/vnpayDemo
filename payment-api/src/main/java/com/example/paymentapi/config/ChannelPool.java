package com.example.paymentapi.config;

import com.example.paymentapi.exception.RequestException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Configuration;

import java.util.NoSuchElementException;

@Slf4j
@Configuration
public class ChannelPool {
    private GenericObjectPool<Channel> internalPool;
    public static final GenericObjectPoolConfig defaultConfig;

    static {
        defaultConfig = new GenericObjectPoolConfig();
        defaultConfig.setMaxTotal(10);
        defaultConfig.setMinIdle(4);
        defaultConfig.setMaxIdle(9);
    }

    public ChannelPool() {
        this(defaultConfig, new ChannelFactory());
    }

    public ChannelPool(final GenericObjectPoolConfig poolConfig, ChannelFactory factory) {
        if (this.internalPool != null) {
            try {
                log.info("Close pool");
                closeInternalPool();
            } catch (Exception e) {
                log.info("Error closeInternalPool");
            }
        }
        log.info("Generate new pool ");
        this.internalPool = new GenericObjectPool<>(factory, poolConfig);
    }

    private void closeInternalPool() {
        try {
            log.info("Close pool");
            internalPool.close();
        } catch (Exception e) {
            log.error("Close pool fail");
            throw new RequestException("61", "Could not destroy the pool");
        }
    }

    public void returnChannel(Channel channel) {
        try {
            if (channel.isOpen()) {
                log.info(" Close channel ");
                internalPool.returnObject(channel);
            } else {
                log.info("Invalid channel");
                internalPool.invalidateObject(channel);
            }
        } catch (Exception e) {
            log.error("Close channel fail!");
            throw new RequestException("62", "Could not return the resource to the pool");
        }
    }

    public Channel getChannel() {
        try {
            return internalPool.borrowObject();
        } catch (NoSuchElementException nse) {
            if (null == nse.getCause()) { // The exception was caused by an exhausted pool
                throw new RequestException("63", "Could not get a resource since the pool is exhausted");
            }
            // Otherwise, the exception was caused by the implemented activateObject() or ValidateObject()
            throw new RequestException("64", "Could not get a resource from the pool");
        } catch (Exception e) {
            throw new RequestException("65", "Could not get a resource from the pool");
        }
    }
}
