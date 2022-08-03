package com.example.paymentapi.service.impl;

import com.example.paymentapi.config.ChannelPool;
import com.example.paymentapi.config.RabbitMQConfig;
import com.example.paymentapi.config.RedisPool;
import com.example.paymentapi.config.YAMLConfig;
import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.Bank;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import com.example.paymentapi.service.IPaymentService;
import com.example.paymentapi.util.ConvertUtils;
import com.example.paymentapi.util.DateTimeUtils;
import com.example.paymentapi.util.ErrorCode;
import com.example.paymentapi.util.MessageResponse;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final YAMLConfig yamlConfig;
    private final Gson gson;
    private final RabbitMQConfig rabbitMQConfig;
    private final RedisPool redisPool;
    private final ErrorCode errorCode;
    private final ChannelPool channelPool;


    //Constructor Injection
    public PaymentServiceImpl(YAMLConfig yamlConfig, Gson gson, RedisPool redisPool, RabbitMQConfig rabbitMQConfig,
                              ErrorCode errorCode, ChannelPool channelPool) {
        this.yamlConfig = yamlConfig;
        this.gson = gson;
        this.redisPool = redisPool;
        this.rabbitMQConfig = rabbitMQConfig;
        this.errorCode = errorCode;
        this.channelPool = channelPool;
    }

    @Override
    public ResponseEntity<ResponseObject> sendRequest(PaymentRequest paymentRequest, BindingResult bindingResult,
                                                      String responseId) {

        log.info("Begin send request with data: {}", paymentRequest);
        String response = null;
        try {
            log.info("Begin validate request");
            validateRequest(paymentRequest, bindingResult);
        } catch (RequestException e) {
            log.info(" Send request have error code: {}", e.getCode());
            return new MessageResponse().bodyErrorResponse(e.getCode(), errorCode.getDescription(e.getCode()),
                    responseId, "", "");
        }

        try {
            log.info("Request is valid => send message to RabbitMQ");
            response = sendMessage(paymentRequest);
        } catch (Exception e){
            log.error("Got exception {}", e.getMessage());
            return new MessageResponse().bodyErrorResponse(ErrorCode.CONNECT_RABBITMQ_FAIL, errorCode.
                    getDescription(ErrorCode.CONNECT_RABBITMQ_FAIL), responseId, "", "");
        }

        return new MessageResponse().bodyResponse(responseId, response, "", "");
//        try {
//            log.info("Begin validate request");
//            validateRequest(paymentRequest, bindingResult);
//            log.info("Request is valid => send message to RabbitMQ");
//            String response = sendMessage(paymentRequest);
//
//            return new MessageResponse().bodyResponse(responseId, response, "", "");
//        } catch (RequestException e) {
//            log.info(" Send request have error code: {}", e.getCode());
//            return new MessageResponse().bodyErrorResponse(e.getCode(), errorCode.getDescription(e.getCode()),
//                    responseId, "", "");
//        } catch (Exception e) {
//            log.error("Got exception {}", e.getMessage());
//            return new MessageResponse().bodyErrorResponse(ErrorCode.CONNECT_RABBITMQ_FAIL, errorCode.
//                    getDescription(ErrorCode.CONNECT_RABBITMQ_FAIL), responseId, "", "");
//        }

    }

    public String sendMessage(PaymentRequest paymentRequest) {
        log.info("Begin publish message to queue with data {} ", paymentRequest);

        Channel channel = null;
        String message;
        try {
            channel = channelPool.getChannel();
            //TODO doc 1 lan lay config
            String rpcQueue = rabbitMQConfig.readConfigFile().getQueue();
            final String corrId = UUID.randomUUID().toString();

            //TODO tao 1 queue rieng de nhan tin
            String replyQueueName = channel.queueDeclare().getQueue();
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(corrId)
                    .replyTo(replyQueueName)
                    .build();

            log.info("correlationId with message {} ", corrId);
            //Convert payment request to String to publish message
            //TODO su dung singleton + ten ham
            message = ConvertUtils.convertObjToString(paymentRequest);
            log.info("Send message to queue {} with data: {}", rpcQueue, message);
            //Send request to queue
            channel.basicPublish("", rpcQueue, props, message.getBytes(StandardCharsets.UTF_8));

            log.info("End send message to server => wait result response from server");
            return receiveMessage(corrId, replyQueueName, channel);
        } catch (Exception e) {
            log.error("Connect to RabbitMQ fail!");
            throw new RequestException(ErrorCode.CONNECT_RABBITMQ_FAIL);
        } finally {
            try {
                if (channelPool != null) {
                    log.info("Return channel to pool");
                    channelPool.returnChannel(channel);
                }
            } catch (Exception e) {
                log.error("Return channel to pool fail");
                throw new RequestException(ErrorCode.CLOSE_CHANNEL_FAIL);
            }

        }
    }

    public String receiveMessage(String corrId, String replyQueueName, Channel channel) throws IOException {

        log.info("Begin receive message RabbitMQ from server with Correlation Id: {}", corrId);
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {

            try {
                if (corrId.equals(delivery.getProperties().getCorrelationId())) {
                    //Chèn phần tử được chỉ định vào ArrayBlockingQueue. Nó trả về false nếu queue đã đầy.
                    response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8), 50, TimeUnit.SECONDS);
                }

            } catch (Exception e) {
                log.error("Get result from server fail");
                throw new RequestException(ErrorCode.GET_RESPONSE_FAIL);
            }
        };

        String ctag = channel.basicConsume(replyQueueName, false, deliverCallback, consumerTag -> {
        });

        log.info("Ctag {}", ctag);

        String result2;
        try {
            result2 = response.poll(30, TimeUnit.SECONDS);
            log.info("Get result from server: {} ", result2);
            channel.basicCancel(ctag);
        } catch (Exception e) {
            log.error("Get result from server is time out");
            throw new RequestException(ErrorCode.GET_RESULT_TIME_OUT);
        }

        log.info("Get result from server success");
        return result2;
    }

    public void validateRequest(PaymentRequest paymentRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.error("One or more field request is empty or null");
            throw new RequestException(ErrorCode.NULL_REQUEST);
        }

        String privateKey = getPrivateKeyByBankCode(paymentRequest.getBankCode());
        if (privateKey == null) {
            throw new RequestException(ErrorCode.NOT_HAVE_PRIVATE_KEY);
        }

        //True if pay date have right format
        if (!DateTimeUtils.isPayDateValid(paymentRequest.getPayDate())) {
            throw new RequestException(ErrorCode.INVALID_DATE_FORMAT);
        }

        checkValidPromotionCode(paymentRequest);

        if (!checkValidAmount(paymentRequest)) {
            log.info("Real amount is invalid (amount > debit amount)");
            throw new RequestException(ErrorCode.INVALID_AMOUNT);
        }

        log.info("Check amount success");

        //True if checksum success
        if (!checkSumSHA256(paymentRequest, privateKey)) {
            throw new RequestException(ErrorCode.CHECK_SUM_ERROR);
        }

        //True if Token key not exist on day
        if (!checkTokenKey(paymentRequest)) {
            throw new RequestException(ErrorCode.DUPLICATE_TOKEN_KEY);
        }
    }

    public boolean checkTokenKey(PaymentRequest paymentRequest) {
        String tokenKey = paymentRequest.getTokenKey();
        log.info("Begin check valid of Token key {}", tokenKey);

        String jsonValue;
        try (Jedis jedis = redisPool.getJedis()) {
            jsonValue = gson.toJson(paymentRequest);
            boolean checkKeyExist = jedis.exists(tokenKey);
            log.info("Token key exist? {}", checkKeyExist);

            if (checkKeyExist) {
                log.info("Token key exist on day");
                return false;
            }

            log.info("Token key can use on day");
            log.info("Setnx to redis with data: {}", jsonValue);

            jedis.setnx(tokenKey, jsonValue);
            jedis.expire(tokenKey, DateTimeUtils.getTimeExpire());

            log.info("End check Token Key success");
            return true;
        } catch (RequestException e) {
            log.error("Connect to Redis fail! ");
            throw new RequestException(ErrorCode.CONNECT_REDIS_FAIL);
        }
    }

    public boolean checkValidAmount(PaymentRequest paymentRequest) {
        log.info("Begin check valid amount");
        return paymentRequest.getRealAmount() <= paymentRequest.getDebitAmount();
    }

    public void checkValidPromotionCode(PaymentRequest paymentRequest) {
        String promotionCode = paymentRequest.getPromotionCode();
        log.info("Begin check valid promotion code with promotionCode {}", promotionCode);
        if (promotionCode == null || promotionCode.isEmpty() || promotionCode.isBlank()) {
            if (paymentRequest.getRealAmount() != paymentRequest.getDebitAmount()) {
                log.info("Real amount different debit amount but promotion code is null, empty or blank => invalid promotion code");
                throw new RequestException(ErrorCode.INVALID_PROMOTION_CODE);
            }
        }
        log.info("End check promotion code success");
    }

    public String getPrivateKeyByBankCode(String bankCode) {
        log.info("Begin getPrivateKey() with bank code: [{}]", bankCode);
        List<Bank> lstBank = yamlConfig.getAllBanks();
        for (Bank item : lstBank) {
            if (item.getBankCode().equalsIgnoreCase(bankCode)) {
                log.info("End getPrivateKeyByBankCode() Have Bank Code in YAML file");
                return item.getPrivateKey();
            }
        }
        log.info("End getPrivateKeyByBankCode() Not have bank code in YAML file");
        return null;
    }

    public StringBuilder getStringToHash(PaymentRequest paymentRequest, String privateKey) {
        log.info("Begin getStringToHash with data PaymentRequest {}", paymentRequest);
        StringBuilder stringBuilder = new StringBuilder();
        log.info("Private key: {}", privateKey);
        log.info("Append to String to HASH");
        stringBuilder.append(paymentRequest.getBankCode())
                .append(paymentRequest.getMobile())
                .append(paymentRequest.getAccountNo())
                .append(paymentRequest.getPayDate())
                .append(paymentRequest.getDebitAmount())
                .append(paymentRequest.getRespCode())
                .append(paymentRequest.getTraceTransfer())
                .append(paymentRequest.getMessageType())
                .append(privateKey);
        log.info("End getStringToHash success");
        return stringBuilder;
    }

    public boolean checkSumSHA256(PaymentRequest paymentRequest, String privateKey) {
        log.info(" Begin checkSumSHA256() with data payment request {}", paymentRequest);
        String sha256hex = Hashing.sha256()
                .hashString(this.getStringToHash(paymentRequest, privateKey), StandardCharsets.UTF_8)
                .toString();

        log.info("Check sum SHA256:[{}] ", sha256hex);
        if (sha256hex.equalsIgnoreCase(paymentRequest.getCheckSum())) {
            log.info("Checksum success");
            return true;
        }

        log.info("Check sum error cause one or more fields are change");
        return false;
    }
}

