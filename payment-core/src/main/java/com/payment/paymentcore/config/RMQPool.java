package com.payment.paymentcore.config;

import com.payment.paymentcore.model.RabbitMQModel;
import com.payment.paymentcore.util.PaymentException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class RMQPool {
    private static final String FILE_CONFIG = "\\config\\rabbitmq-config.properties";
    private Connection connection;
    private List<Channel> channels;
    private static RMQPool instance;

    public RMQPool() throws IOException, TimeoutException {
        awakeConnection();
    }

    public static RMQPool getInstance() throws IOException, TimeoutException {
        if (instance == null) {
            instance = new RMQPool();
        }
        return instance;
    }

    public RabbitMQModel readConfigFile() throws PaymentException{
        Properties properties = new Properties();
        InputStream inputStream = null;
        RabbitMQModel rabbitMQModel = new RabbitMQModel();
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            // get property by name
            rabbitMQModel.setUserName(properties.getProperty("username"));
            rabbitMQModel.setPassword(properties.getProperty("password"));
            rabbitMQModel.setHost(properties.getProperty("host"));
            rabbitMQModel.setQueue(properties.getProperty("queue"));
            rabbitMQModel.setPort(Integer.parseInt(properties.getProperty("port")));
            rabbitMQModel.setMaxChannel(Integer.parseInt(properties.getProperty("max_channel")));
            rabbitMQModel.setTimeOut(Integer.parseInt(properties.getProperty("timeout")));

        } catch (IOException e) {
            e.printStackTrace();
        }catch (PaymentException e){
            throw new PaymentException("34", "Read config file RabbitMQ fail");
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
        return rabbitMQModel;
    }

    private boolean awakeConnection() throws IOException, TimeoutException {
        try{
            RabbitMQModel rabbitMQModel = new RabbitMQModel();
            rabbitMQModel = readConfigFile();
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(rabbitMQModel.getUserName());
            factory.setPassword(rabbitMQModel.getPassword());
            factory.setHost(rabbitMQModel.getHost());
            factory.setPort(rabbitMQModel.getPort());

            connection = factory.newConnection();
            if (channels != null) {
                channels.clear();

            } else {
                channels = new ArrayList<Channel>();
            }
            for (int i = 0; i < readConfigFile().getMaxChannel() ; i++) {
                spawnChannel();
            }

            return true;
        }catch (PaymentException e){
            throw new PaymentException("46", "Awake Connection fail");
        }

    }

    private void spawnChannel() throws IOException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(readConfigFile().getQueue(), true, false, false, null);
        channels.add(channel);
    }

    public Channel getChannel() throws IOException, TimeoutException {
        if (this.connection.isOpen()) {
            while (channels.size() == 0) {
                try {
                    channels.wait(readConfigFile().getTimeOut());

                } catch (InterruptedException e) {
                    throw new PaymentException("78", "Get channel is time out");
                }
                if (channels.size() == 0) {
                    spawnChannel();
                }
            }
            return channels.remove(0);

        } else {
            awakeConnection();
            return getChannel();
        }
    }

    public void releaseChannel(Channel channel) throws IOException {
        if (channel.isOpen()) {
            channels.add(channel);
            channels.notifyAll();

        } else if (channels.size() < readConfigFile().getMaxChannel()) {
            spawnChannel();
        }
    }
}
