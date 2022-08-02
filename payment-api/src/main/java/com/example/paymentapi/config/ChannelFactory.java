package com.example.paymentapi.config;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.util.ErrorCode;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

@Slf4j
public class ChannelFactory implements PooledObjectFactory<Channel> {
    private final Connection connection;

    public ChannelFactory() {
        this(null);
    }

    public ChannelFactory(String uri) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            //factory.setUsername("guest");
            factory.setHost("localhost");
            factory.setPort(5672);
            if (uri != null) {
                factory.setUri(uri);
            }

            //TODO dang tao nguoc channel -> connection can sua lai
            connection = factory.newConnection();
        } catch (Exception e) {
            throw new RequestException(ErrorCode.CONNECT_RABBITMQ_FAIL);
        }
    }

    public PooledObject<Channel> makeObject() throws Exception {
        return new DefaultPooledObject<>(connection.createChannel());
    }

    public void destroyObject(PooledObject<Channel> pooledObject) throws Exception {
        final Channel channel = pooledObject.getObject();
        if (channel.isOpen()) {
            try {
                channel.close();
            } catch (Exception e) {
                log.info("Destroy object fail");
            }
        }
    }

    public boolean validateObject(PooledObject<Channel> pooledObject) {
        final Channel channel = pooledObject.getObject();
        return channel.isOpen();
    }

    public void activateObject(PooledObject<Channel> pooledObject) {

    }

    public void passivateObject(PooledObject<Channel> pooledObject) {

    }
}
