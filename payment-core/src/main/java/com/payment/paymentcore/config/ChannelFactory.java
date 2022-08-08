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
    private static Connection connection;

    private static final String HOST = "localhost";
    private static final int PORT = 5672;

    public ChannelFactory() {
        this(null);
    }

    public ChannelFactory(String uri) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
//            factory.setUsername("guest");
            factory.setAutomaticRecoveryEnabled(true);
            factory.setHost(HOST);
            factory.setPort(PORT);
            log.info("Create Connection {}", factory);
            connection = factory.newConnection();
            log.info("Create Connection {}", connection);
        } catch (Exception e) {
            log.error("Exception when create connection factory {}", e);
            throw new PaymentException(ErrorCode.CONNECT_RABBITMQ_FAIL);
        }
    }

    /**
     * 44 * Initialize a connection factory
     * 45 * @param rabbitMqName
     * 46
     */

    public PooledObject<Channel> makeObject() throws Exception {
        return new DefaultPooledObject<>(connection.createChannel());
    }

    /**
     *
     * @param pooledObject a {@code PooledObject} wrapping the instance to be destroyed
     *
     */
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

    /**
     *
     * @param pooledObject a {@code PooledObject} wrapping the instance to be validated
     *
     */
    public boolean validateObject(PooledObject<Channel> pooledObject) {
        final Channel channel = pooledObject.getObject();
        return channel.isOpen();
    }

    public void activateObject(PooledObject<Channel> pooledObject) {

    }

    public void passivateObject(PooledObject<Channel> pooledObject) {

    }
}
