package com.example.paymentapi.config;

import com.example.paymentapi.exception.PaymentException;
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
        defaultConfig.setMaxTotal(13);
        defaultConfig.setMinIdle(4);
        defaultConfig.setMaxIdle(9);
    }

    public ChannelPool() {
        this(defaultConfig, new ChannelFactory());
    }

    public ChannelPool(final GenericObjectPoolConfig poolConfig, ChannelFactory factory) {
        if (this.internalPool != null) {
            try {
                log.info("Close pool rabbitmq");
                closeInternalPool();
            } catch (Exception e) {
                log.info("Error closeInternalPool rabbitmq");
            }
        }
        log.info("Generate new pool rabbitmq");
        this.internalPool = new GenericObjectPool<>(factory, poolConfig);
    }

    private void closeInternalPool() {
        try {
            log.info("Close pool rabbitmq");
            internalPool.close();
        } catch (Exception e) {
            log.error("Close pool rabbitmq fail");
            throw new PaymentException("61", "Could not destroy the pool");
        }
    }

    public void returnChannel(Channel channel) {
        try {
            if (channel.isOpen()) {
                log.info(" Close channel rabbitmq {}" , channel);
                internalPool.returnObject(channel);
            } else {
                log.info("Invalid channel rabbitmq");
                internalPool.invalidateObject(channel);
            }
        } catch (Exception e) {
            log.error("Close channel rabbitmq fail!");
            throw new PaymentException("62", "Could not return the resource to the pool");
        }
    }

    public Channel getChannel() {
        try {
            log.info("Get channel() ");
            return internalPool.borrowObject();
        } catch (NoSuchElementException nse) {
            if (null == nse.getCause()) { // The exception was caused by an exhausted pool
                log.error("Could not get a resource since the pool is exhausted {}", nse);
                throw new PaymentException("63", "Could not get a resource since the pool is exhausted");
            }
            // Otherwise, the exception was caused by the implemented activateObject() or ValidateObject()
            log.error("Could not get a resource from the pool");
            throw new PaymentException("64", "Could not get a resource from the pool");
        } catch (Exception e) {
            log.error("Could not get a resource from the pool {}", e);
            throw new PaymentException("65", "Could not get a resource from the pool");
        }
    }
}
