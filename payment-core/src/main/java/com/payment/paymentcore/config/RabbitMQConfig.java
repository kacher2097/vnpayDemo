package com.payment.paymentcore.config;

import com.payment.paymentcore.DAO.PaymentDAO;
import com.payment.paymentcore.model.PaymentRequest;
import com.payment.paymentcore.service.PaymentService;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQConfig {

    public static final String HOST = "localhost";
    public static final String USER_NAME = "guest";
    public static final String PASS_WORD = "guest";
    public static final int PORT = 49154;
    //    public static final String QUEUE = "queue.payment";
    private String rpcQueue = "queue.rpc";
    public static final String EXCHANGE = "payment.exchange";
    public static final String ROUTING_KEY = "payment.routingkey";

    private static RabbitMQConfig instance;
    private static final Logger log = LogManager.getLogger(RabbitMQConfig.class);

    public static RabbitMQConfig getInstance() throws IOException, TimeoutException {
        if (instance == null) {
            instance = new RabbitMQConfig();
        }
        return instance;
    }

    public void sendAndReceiveMessage() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);

        RMQPool rmqPool = null;
        try {
            rmqPool = new RMQPool();
            Channel channel = rmqPool.getChannel();
            channel.queueDeclare(rpcQueue, true, false, false, null);
            channel.queuePurge(rpcQueue);
            channel.basicQos(1);

            log.info(" [x] Awaiting RPC requests");

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";
                log.info("getCorrelationId: {}", delivery.getProperties().getCorrelationId());
                try {
                    PaymentService paymentService = new PaymentService();
                    PaymentDAO paymentDAO = new PaymentDAO();
                    String message = new String(delivery.getBody(), "UTF-8");

                    log.info("Receive Message: " + message);
                    PaymentRequest paymentRequest = paymentService.convertJsonMessageToObject(message);

                    //   if (paymentDAO.addPaymentRequest(paymentRequest)) {

//                  log.info("insert to DB success");
//                  log.info("Next => send to api and get response");
//                  paymentService.sendApi(paymentRequest);

                    response += "Status response: " + paymentService.sendApi(paymentRequest);
                    log.info("response: {}", response);

//                   } else {
//                      log.info("Add fail");
//                   }

                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e.toString());
                    throw new RuntimeException(e);
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
                            response.getBytes("UTF-8"));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    log.info("delivery.getEnvelope().getDeliveryTag(): {}",
                            String.valueOf(delivery.getEnvelope().getDeliveryTag()));
                    // RabbitMq consumer worker thread notifies the RPC server owner thread
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };
            channel.basicConsume(rpcQueue, false, deliverCallback, (consumerTag -> {
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
        } catch (RuntimeException e){

        }
    }
}


//    public void ReceiveRequest() throws IOException, TimeoutException {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost(HOST);
//        factory.setPort(PORT);
//
//        RMQPool rmqPool = new RMQPool();
//        try (Connection connection = factory.newConnection();
//             Channel channel = connection.createChannel()) {
//            channel.queueDeclare(rpcQueue, true, false, false, null);
//            channel.queuePurge(rpcQueue);
//            channel.basicQos(1);
//            System.out.println(" [x] Awaiting RPC requests");
//
//            Object monitor = new Object();
//            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
//                        .Builder()
//                        .correlationId(delivery.getProperties().getCorrelationId())
//                        .build();
//
//                String response = "";
//                log.info("getCorrelationId: {}", delivery.getProperties().getCorrelationId());
//                try {
//                    PaymentService paymentService = new PaymentService();
//                    PaymentDAO paymentDAO = new PaymentDAO();
//                    String message = new String(delivery.getBody(), "UTF-8");
//
//                    System.out.println("Message: " + message);
//                    PaymentRequest paymentRequest = paymentService.convertJsonMessageToObject(message);
//
////                        if (paymentDAO.addPaymentRequest(paymentRequest)) {
//
////                            log.info("insert to DB success");
////                            log.info("Next => send to api and get response");
////                            paymentService.sendApi(paymentRequest);
//                        response += paymentService.sendApi(paymentRequest);
//                        log.info("response: {}", response);
//
////                        } else {
////                            log.info("Add fail");
////                        }
//
//                } catch (RuntimeException e) {
//                    System.out.println(" [.] " + e.toString());
//                } finally {
//                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
//                            response.getBytes("UTF-8"));
//                    log.info("replyto: {}", delivery.getProperties().getReplyTo());
//                    log.info("replyProps: {}", replyProps);
//                    log.info("response: {}", response.getBytes());
//                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//                    log.info("delivery.getEnvelope().getDeliveryTag(): {}",
//                            String.valueOf(delivery.getEnvelope().getDeliveryTag()));
//                    // RabbitMq consumer worker thread notifies the RPC server owner thread
//                    synchronized (monitor) {
//                        monitor.notify();
//                    }
//                }
//            };
////            channel.basicConsume(QUEUE, false, (Consumer) deliverCallback);
//            channel.basicConsume(rpcQueue, false, deliverCallback, (consumerTag  ->  {
//            }));
//
//            // Wait and be prepared to consume the message from RPC client.
//            while (true) {
//                synchronized (monitor) {
//                    try {
//                        monitor.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }


//        channel.queueBind(QUEUE, EXCHANGE, ROUTING_KEY);
//        try {
//            DeliverCallback deliverCallback = new DeliverCallback() {
//                @Override
//                public void handle(String consumerTag, Delivery delivery) throws IOException {
//                    PaymentService paymentService = new PaymentService();
//                    PaymentDAO paymentDAO = new PaymentDAO();
//                    String message = new String(delivery.getBody(), "UTF-8");
//                    System.out.println(" [x] Received: '" + message + "'");
//                    PaymentRequest paymentRequest = paymentService.convertJsonMessageToObject(message);
//
//                    if(paymentDAO.addPaymentRequest(paymentRequest)){
//                        log.info("insert to DB success");
//                        log.info("Next => send to api and get response");
//                        paymentService.sendApi(paymentRequest);
//
//                    }else {
//                        System.out.println("Add fail");
//                    }
//
//                }
//            };
//            CancelCallback cancelCallback = new CancelCallback() {
//                @Override
//                public void handle(String consumerTag) throws IOException {
//                }
//            };
//            String consumerTag = channel.basicConsume(QUEUE, true, deliverCallback, cancelCallback);
//            System.out.println("consumerTag: " + consumerTag);

//        }catch (Exception e){
//            e.getMessage();
//        }
//    }

