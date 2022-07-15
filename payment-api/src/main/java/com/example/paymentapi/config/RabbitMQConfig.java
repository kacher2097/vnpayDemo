package com.example.paymentapi.config;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.RabbitMQProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

@Configuration
public class RabbitMQConfig {
    private static final String FILE_CONFIG = "\\config\\rabbitmq-config.properties";
    private Connection connection;
    private List<Channel> channels;

    //TODO phai co connection pool đã có channel pool
    public RabbitMQConfig() throws IOException, TimeoutException {
        awakeConnection();
    }

    public RabbitMQProperties readConfigFile(){
        Properties properties = new Properties();
        InputStream inputStream = null;
        RabbitMQProperties rabbitMQProperties = new RabbitMQProperties();
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            // get property by name
            rabbitMQProperties.setUserName(properties.getProperty("username"));
            rabbitMQProperties.setPassword(properties.getProperty("password"));
            rabbitMQProperties.setHost(properties.getProperty("host"));
            rabbitMQProperties.setQueue(properties.getProperty("queue"));
            rabbitMQProperties.setPort(Integer.parseInt(properties.getProperty("port")));
            rabbitMQProperties.setMaxChannel(Integer.parseInt(properties.getProperty("max_channel")));
            rabbitMQProperties.setTimeOut(Integer.parseInt(properties.getProperty("timeout")));

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
        return rabbitMQProperties;
    }

    private boolean awakeConnection() throws IOException, TimeoutException {
        RabbitMQProperties rabbitMQProperties = readConfigFile();
        com.rabbitmq.client.ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(rabbitMQProperties.getUserName());
        factory.setPassword(rabbitMQProperties.getPassword());
        factory.setHost(rabbitMQProperties.getHost());
        factory.setPort(rabbitMQProperties.getPort());


        connection = factory.newConnection();

        if (channels != null) {
            channels.clear();

        } else {
            channels = new ArrayList<>();
        }
        for (int i = 0; i < readConfigFile().getMaxChannel() ; i++) {
            spawnChannel();
        }

        return true;
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
                    throw new RequestException("29", "Get channel is time out");
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
