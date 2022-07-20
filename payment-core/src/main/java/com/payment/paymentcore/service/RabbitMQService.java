package com.payment.paymentcore.service;

import com.payment.paymentcore.DAO.PaymentDAO;
import com.payment.paymentcore.config.RMQPool;
import com.payment.paymentcore.model.PaymentRequest;
import com.payment.paymentcore.util.Convert;
import com.payment.paymentcore.util.PaymentException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
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
    private static final PaymentDAO paymentDao;

    static {
        try {
            rmqPool = RMQPool.getInstance();
            paymentDao = PaymentDAO.getInstance();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public Channel connectToRabbitMQ() throws IOException, TimeoutException, PaymentException {
        String rpcQueue = rmqPool.readConfigFile().getQueue();
        try {
            Channel channel = rmqPool.getChannel();
            channel.queueDeclare(rpcQueue, true, false, false, null);
            channel.basicQos(1);
            return channel;
        } catch (PaymentException e) {
            log.error("Create channel fail");
            throw new PaymentException("77", "Create channel RabbitMQ fail");
        }
    }

//    public String messageToClient(String message) throws SQLException {
//        String responseToClient = "";
//        PaymentService paymentService = new PaymentService();
////        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
//        log.info("Receive message from queue: " + message);
//
//        PaymentRequest paymentRequest = Convert.convertJsonMessageToObject(message);
//        try {
//            if (paymentDao.addPaymentRequest(paymentRequest)) {
//
//                log.info("insert to DB success => send to api and get response");
//                String response = " Status response " + paymentService.sendApi(paymentRequest);
//                log.info("response from partner api: {}", response);
//
//                //Convert response from api partner to JSON string for send response to client
//                responseToClient = Convert.convertObjToString(new PaymentException("00",
//                        "Response from partner api:" + response));
//
//            } else {
//                log.info("Insert into DB fail");
//                //Convert response  & exception from api partner to JSON string for send response to client
//                responseToClient = Convert.convertObjToString(new PaymentException("95",
//                        "Insert into DB fail"));
//            }
//        } catch (SQLException e) {
//
//        }
//        //true if execute query success
//        return responseToClient;
//    }

    public void sendAndReceiveMessage() {
        try {
            Channel channel = this.connectToRabbitMQ();
            log.info(" [x] Awaiting RPC requests");

//            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String correlationIdFromClient = delivery.getProperties().getCorrelationId();
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(correlationIdFromClient)
                        .build();

                String responseToClient = "";
                log.info("getCorrelationId from client: {}", correlationIdFromClient);
                try {
                    PaymentService paymentService = new PaymentService();
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    log.info("Receive Message from queue: " + message);

                    PaymentRequest paymentRequest = Convert.convertJsonMessageToObject(message);

                    //true if execute query success
                    if (paymentDao.addPaymentRequest(paymentRequest)) {

                        log.info("insert to DB success => send to api and get response");
                        String response = " Status response " + paymentService.sendApi(paymentRequest);
                        log.info("response from partner api: {}", response);

                        //Convert response from api partner to JSON string for send response to client
                        responseToClient = Convert.convertObjToString(new PaymentException("00",
                                "Response from partner api:" + response));

                    } else {
                        log.info("Insert into DB fail");
                        //Convert response  & exception from api partner to JSON string for send response to client
                        responseToClient = Convert.convertObjToString(new PaymentException("95",
                                "Insert into DB fail"));
                    }

                } catch (PaymentException e) {
                    log.error("Payment exception with error code : {}", e.getCode());
                    //Convert response & exception from api partner to JSON string for send response to client
                    responseToClient = Convert.convertObjToString(new PaymentException(e.getCode(), e.getMessage()));

                } catch (SQLException e) {
                    //Convert response  & exception from api partner to JSON string for send response to client
                    log.error("SQL exception {}", e);
                    responseToClient = Convert.convertObjToString(new PaymentException("55", e.getMessage()));
                } finally {
                    log.info("Publish response to client with data: {}", responseToClient);
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
                            responseToClient.getBytes(StandardCharsets.UTF_8));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                    // RabbitMq consumer worker thread notifies the RPC server owner thread
//                    synchronized (monitor) {
//                        monitor.notify();
//                    }
                }
            };
            channel.basicConsume(rmqPool.readConfigFile().getQueue(), false, deliverCallback, (consumerTag -> {
            }));

            // Wait and be prepared to consume the message from RPC client.
//            while (true) {
//                synchronized (monitor) {
//                    try {
//                        monitor.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
        } catch (Exception e) {
            log.error("Have exception on RabbitMQ {} ", e);
            e.printStackTrace();
        }
    }
}


