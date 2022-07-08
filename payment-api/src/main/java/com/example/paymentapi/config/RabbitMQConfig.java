package com.example.paymentapi.config;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.RabbitMQModel;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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
    private static RabbitMQConfig instance;

    public RabbitMQConfig() throws IOException, TimeoutException {
        awakeConnection();
    }

    public static RabbitMQConfig getInstance() throws IOException, TimeoutException {
        if (instance == null) {
            instance = new RabbitMQConfig();
        }
        return instance;
    }

    public RabbitMQModel readConfigFile(){
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
        RabbitMQModel rabbitMQModel = new RabbitMQModel();
        rabbitMQModel = readConfigFile();
        com.rabbitmq.client.ConnectionFactory factory = new ConnectionFactory();
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
                    throw new RequestException("", "Get channel is time out");
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

//    @Value("${spring.rabbitmq.host}")
//    private String host;
//
//    @Value("${spring.rabbitmq.username}")
//    private String userName;
//
//    @Value("${spring.rabbitmq.password}")
//    private String password;
//
//    @Value("${spring.rabbitmq.queue}")
//    private String queue;
//
//    @Value("${spring.rabbitmq.exchange}")
//    private String exchange;
//
//    @Value("${spring.rabbitmq.routingkey}")
//    private String routingKey;
//
//    @Bean
//    Queue queue(){
//        return new Queue(queue, true);
//    }
//
//    @Bean
//    Exchange myExchange(){
//        return ExchangeBuilder.directExchange(exchange).durable(true).build();
//    }
//
//    @Bean
//    Binding binding(){
//        return BindingBuilder
//                .bind(queue())
//                .to(myExchange())
//                .with(routingKey)
//                .noargs();
//    }
//
//    @Bean
//    public ConnectionFactory connectionFactory(){
//        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(host);
//        cachingConnectionFactory.setUsername(userName);
//        cachingConnectionFactory.setPassword(password);
//        cachingConnectionFactory.setPort(49154);
//
//        return cachingConnectionFactory;
//    }
//
//    @Bean
//    public MessageConverter jsonMessageConverter(){
//        return new Jackson2JsonMessageConverter();
//    }
//
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//        rabbitTemplate.setMessageConverter(jsonMessageConverter());
//
//        //
//        rabbitTemplate.setReplyAddress(queue);
//        rabbitTemplate.setReplyTimeout(6000);
//        return rabbitTemplate;
//    }
//
//    @Bean
//    SimpleMessageListenerContainer replyContainer(ConnectionFactory connectionFactory) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setQueueNames(queue);
//        container.setMessageListener(rabbitTemplate(connectionFactory));
//        return container;
//    }
}
