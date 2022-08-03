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
        defaultConfig.setMaxTotal(10);
        defaultConfig.setMaxIdle(9);
        defaultConfig.setMinIdle(3);
    }

    public ChannelPool() {
        this(defaultConfig, new ChannelFactory());
    }

    public ChannelPool(final GenericObjectPoolConfig poolConfig, ChannelFactory factory) {
        if (this.internalPool != null) {
            try {
                closeInternalPool();
            } catch (Exception e) {
                log.error("Exception internalPool {}", e);
            }
        }

        this.internalPool = new GenericObjectPool<Channel>(factory, poolConfig);
    }

    private void closeInternalPool() {
        try {
            internalPool.close();
        } catch (Exception e) {
            throw new PaymentException("71", "Could not destroy the pool on server");
        }
    }

    public void returnChannel(Channel channel) {
        try {
            if (channel.isOpen()) {
                internalPool.returnObject(channel);
            } else {
                internalPool.invalidateObject(channel);
            }
        } catch (Exception e) {
            throw new PaymentException("72", "Could not return the resource to the pool on server");
        }
    }

    public Channel getChannel() {
        try {
            return internalPool.borrowObject();
        } catch (NoSuchElementException nse) {
            if (null == nse.getCause()) { // The exception was caused by an exhausted pool
                throw new PaymentException("73", "Could not get a resource since the pool is exhausted on server");
            }
            // Otherwise, the exception was caused by the implemented activateObject() or ValidateObject()
            throw new PaymentException("74", "Could not get a resource from the pool on server");
        } catch (Exception e) {
            throw new PaymentException("75", "Could not get a resource from the pool on server");
        }
    }
}
