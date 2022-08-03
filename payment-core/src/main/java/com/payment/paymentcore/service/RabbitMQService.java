package com.payment.paymentcore.service;

import com.payment.paymentcore.DAO.PaymentDAO;
import com.payment.paymentcore.config.ChannelPool;
import com.payment.paymentcore.config.RMQPool;
import com.payment.paymentcore.model.PaymentRequest;
import com.payment.paymentcore.util.Convert;
import com.payment.paymentcore.util.ErrorCode;
import com.payment.paymentcore.util.PaymentException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private static final ChannelPool channelPool;
    private static final PaymentDAO paymentDao;
    private static final ErrorCode errorCode;

    private static final PaymentService paymentService;

    static {
        channelPool = ChannelPool.getInstance();
        paymentDao = PaymentDAO.getInstance();
        errorCode = ErrorCode.getInstance();
        paymentService = PaymentService.getInstance();
    }

    public Channel connectToRabbitMQ() {
        String rpcQueue = RMQPool.readConfigFile().getQueue();
        try {
            Channel channel = channelPool.getChannel();
//            Channel channel = rmqPool.getChannel();
            channel.queueDeclare(rpcQueue, true, false, false, null);
            channel.basicQos(1);
            return channel;
        } catch (Exception e) {
            log.error("Create channel fail");
            throw new PaymentException(ErrorCode.CREATE_CHANNEL_RABBITMQ_FAIL);
        }
    }

    public void consumeAndPublishMessage() {
        try {
            Channel channel = this.connectToRabbitMQ();
            log.info("[x] Awaiting RPC requests");

//            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String correlationIdFromClient = delivery.getProperties().getCorrelationId();
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(correlationIdFromClient)
                        .build();

                //TODO tim cach khac xu li String
                String responseToClient = null;
                log.info("getCorrelationId from client: {}", correlationIdFromClient);
                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    log.info("Receive Message from queue: " + message);

                    //Convert json data from client to object -> insert into database
                    PaymentRequest paymentRequest = Convert.convertJsonMessageToObject(message);

                    //true if execute query success
                    if (paymentDao.addPaymentRequest(paymentRequest)) {

                        log.info("insert to DB success => send to api and get response");

                        String response = " Status response " + paymentService.sendApi(paymentRequest);
                        log.info("response from partner api: {}", response);

                        //Convert response from api partner to JSON string for send response to client
                        responseToClient = Convert.convertObjToJson(new PaymentException(ErrorCode.REQUEST_SUCCESS,
                                "Response from partner api:" + response));

                    } else {
                        log.info("Insert into DB fail => response to client");
                        //Convert response  & exception from api partner to JSON string for send response to client
                        responseToClient = Convert.convertObjToJson(new PaymentException(ErrorCode.INSERT_INTO_DB_FAIL,
                                "Insert into DB fail"));
                    }

                } catch (PaymentException e) {
                    log.info("Payment exception with error code : {}", e.getCode());
                    //Convert response & exception from api partner to JSON string for send response to client
                    responseToClient = Convert.convertObjToJson(new PaymentException(e.getCode(),
                            errorCode.readErrorDescriptionFile(e.getCode())));

                } finally {
                    log.info("Publish response to client with data: {}", responseToClient);
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
                            responseToClient.getBytes(StandardCharsets.UTF_8));

                    //Acknowledge receive message
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    log.info("----- End publish response to client -----");
                    // RabbitMq consumer worker thread notifies the RPC server owner thread
//                    synchronized (monitor) {
//                        monitor.notify();
//                    }
                }
            };


            channel.basicConsume(RMQPool.readConfigFile().getQueue(), false, deliverCallback, (consumerTag -> {
            }));
            //Return channel to pool
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


