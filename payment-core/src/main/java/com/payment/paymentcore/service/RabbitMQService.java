package com.payment.paymentcore.service;

import com.payment.paymentcore.DAO.PaymentDAO;
import com.payment.paymentcore.config.RMQPool;
import com.payment.paymentcore.model.PaymentRequest;
import com.payment.paymentcore.util.Convert;
import com.payment.paymentcore.util.PaymentException;
import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

public class RabbitMQService {
    private static RabbitMQService instance;
    private static final Logger log = LogManager.getLogger(RabbitMQService.class);

    public static RabbitMQService getInstance() throws IOException, TimeoutException {
        if (instance == null) {
            instance = new RabbitMQService();
        }
        return instance;
    }

    private static final RMQPool rmqPool;

    static {
        try {
            rmqPool = RMQPool.getInstance();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public Channel connectToRabbitMQ() throws IOException, TimeoutException, PaymentException {
        //RMQPool rmqPool = new RMQPool();
        String rpcQueue = rmqPool.readConfigFile().getQueue();
        try {
            Channel channel = rmqPool.getChannel();
            channel.queueDeclare(rpcQueue, true, false, false, null);
            //channel.queuePurge(rpcQueue);
            channel.basicQos(1);
            return channel;
        }catch (PaymentException e){
            log.error("Create channel fail");
            throw new PaymentException("77", "Create channel RabbitMQ fail");
        }
    }

    public void sendAndReceiveMessage() throws IOException, TimeoutException {
//        RMQPool rmqPool = new RMQPool();
//        String rpcQueue = rmqPool.readConfigFile().getQueue();
        try {
            Channel channel = this.connectToRabbitMQ();
            log.info(" [x] Awaiting RPC requests");

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String responseToClient = "";
                log.info("getCorrelationId: {}", delivery.getProperties().getCorrelationId());
                try {
                    PaymentService paymentService = new PaymentService();
                    PaymentDAO paymentDAO = new PaymentDAO();

                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    log.info("Receive Message from queue: " + message);

                    PaymentRequest paymentRequest = Convert.convertJsonMessageToObject(message);
                    if (paymentDAO.addPaymentRequest(paymentRequest)) {

                        log.info("insert to DB success => send to api and get response");
                        String response = " Status response " + paymentService.sendApi(paymentRequest);
                        log.info("response from partner api: {}", response);

                        responseToClient = Convert.convertObjToString(new PaymentException("00",
                                "Response from partner api:" + response));

                    } else {
                        log.info("Insert into DB fail");
                        responseToClient = Convert.convertObjToString(new PaymentException("95",
                                "Insert into DB fail"));
                    }

                } catch (PaymentException e) {
                    responseToClient =  Convert.convertObjToString(new PaymentException(e.getCode(), e.getMessage()));
                } catch (Exception e) {
                    responseToClient =  Convert.convertObjToString(new PaymentException("55", e.getMessage()));
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
                            responseToClient.getBytes(StandardCharsets.UTF_8));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                    // RabbitMq consumer worker thread notifies the RPC server owner thread
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };
            channel.basicConsume(rmqPool.readConfigFile().getQueue(), false, deliverCallback, (consumerTag -> {
            }));

            // Wait and be prepared to consume the message from RPC client.
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


