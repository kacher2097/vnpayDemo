package com.payment.paymentcore.config;

import com.payment.paymentcore.util.PaymentException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.NoSuchElementException;

@Slf4j
public class ChannelPool implements Cloneable{

    private static ChannelPool instance;

    private GenericObjectPool<Channel> internalPool;
    public static GenericObjectPoolConfig defaultConfig;

    public static ChannelPool getInstance(){
        if(instance == null){
            instance = new ChannelPool();
        }
        return instance;
    }

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
            throw new PaymentException("61", "Could not destroy the pool");
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
            throw new PaymentException("62", "Could not return the resource to the pool");
        }
    }

    public Channel getChannel() {
        try {
            return internalPool.borrowObject();
        } catch (NoSuchElementException nse) {
            if (null == nse.getCause()) { // The exception was caused by an exhausted pool
                throw new PaymentException("63", "Could not get a resource since the pool is exhausted");
            }
            // Otherwise, the exception was caused by the implemented activateObject() or ValidateObject()
            throw new PaymentException("64", "Could not get a resource from the pool");
        } catch (Exception e) {
            throw new PaymentException("65", "Could not get a resource from the pool");
        }
    }
}
