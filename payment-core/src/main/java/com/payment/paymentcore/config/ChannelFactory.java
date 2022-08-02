package com.payment.paymentcore.config;

import com.payment.paymentcore.util.ErrorCode;
import com.payment.paymentcore.util.PaymentException;
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
//            factory.setUsername("guest");
            factory.setHost("localhost");
            factory.setPort(5672);
            log.info("Create Connection {}", factory);

            //TODO khong su dung != null + logic code xu ly == null ()
            if (uri != null) {
                factory.setUri(uri);
            }

            //TODO mat connection neu bi ngat ket noi -> khoi tao lai connection
            connection = factory.newConnection();
            log.info("Create Connection {}", connection);
        } catch (Exception e) {
            log.error("Exception when create connection factory {}", e);
            throw new PaymentException(ErrorCode.CONNECT_RABBITMQ_FAIL);
        }
    }

    public PooledObject<Channel> makeObject() throws Exception {
        return new DefaultPooledObject<>(connection.createChannel());
    }

    public void destroyObject(PooledObject<Channel> pooledObject) {
        final Channel channel = pooledObject.getObject();
        if (channel.isOpen()) {
            try {
                log.info("Destroy channel ");
                channel.close();
            } catch (Exception e) {
                log.error("Exception when close channel {}", e);
                throw new PaymentException(ErrorCode.DESTROY_CHANNEL_RABBITMQ_FAIL);
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
