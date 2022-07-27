package com.example.paymentapi.service.impl;

import com.example.paymentapi.config.RabbitMQConfig;
import com.example.paymentapi.config.RedisPool;
import com.example.paymentapi.config.YAMLConfig;
import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.Bank;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import com.example.paymentapi.service.IPaymentService;
import com.example.paymentapi.util.Convert;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final YAMLConfig yamlConfig;
    private final Gson gson;
    private final RabbitMQConfig rabbitMQConfig;
    private final RedisPool redisPool;
    private final ErrorCode errorCode;

    //Constructor Injection
    public PaymentServiceImpl(YAMLConfig yamlConfig, Gson gson, RedisPool redisPool, RabbitMQConfig rabbitMQConfig,
                              ErrorCode errorCode) {
        this.yamlConfig = yamlConfig;
        this.gson = gson;
        this.redisPool = redisPool;
        this.rabbitMQConfig = rabbitMQConfig;
        this.errorCode = errorCode;
    }

    @Override
    public ResponseEntity<ResponseObject> sendRequest(PaymentRequest paymentRequest, BindingResult bindingResult,
                                                      String responseId) {

        log.info("Begin send request with data: {}", paymentRequest);

        try {
            validateRequest(paymentRequest, bindingResult);
            String response = callRabbitMQ(paymentRequest);

            return new MessageResponse().bodyResponse(responseId, response, "", "");
        } catch (RequestException e) {
            log.info(" error code: {}", e.getCode());
            if (errorCode.getDescription(e.getCode()) == null) {
                return new MessageResponse().bodyErrorResponse(e.getCode(), e.getMessage(),
                        responseId, "", "");
            }
            return new MessageResponse().bodyErrorResponse(e.getCode(), errorCode.getDescription(e.getCode()),
                    responseId, "", "");
        } catch (Exception e) {
            log.error("Connect to RabbitMQ fail: {} ", e.getMessage());
            return new MessageResponse().bodyErrorResponse(ErrorCode.CONNECT_RABBITMQ_FAIL, errorCode.
                    getDescription(ErrorCode.CONNECT_RABBITMQ_FAIL), responseId, "", "");
        }

    }

    public String callRabbitMQ(PaymentRequest paymentRequest) throws IOException, TimeoutException, InterruptedException {
        log.info("Begin publish message to queue with data {} ", paymentRequest);
        Channel channel = rabbitMQConfig.getChannel();
        String rpcQueue = rabbitMQConfig.readConfigFile().getQueue();
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        log.info("correlationId with message {} ", corrId);
        //Convert payment request to String to publish message
        String message = Convert.convertObjToString(paymentRequest);
        log.info("Send message to queue {} with data: {}", rpcQueue, message);
        //Send request to queue
        channel.basicPublish("", rpcQueue, props, message.getBytes(StandardCharsets.UTF_8));

        //Result receive from server
        //log.info("Result from server {} ", result);
        return receiveMessageRabbitMQ(corrId, replyQueueName, channel);
    }

    public String receiveMessageRabbitMQ(String corrId, String replyQueueName, Channel channel) throws
            InterruptedException, IOException {

        log.info("Begin receive message RabbitMQ from server");
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {

            try {
                if (corrId.equals(delivery.getProperties().getCorrelationId())) {
                    response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
                }

            } catch (Exception e) {
                throw new RequestException("010", "RabbitMQ fail");
            }
        };

        String ctag = channel.basicConsume(replyQueueName, false, deliverCallback, consumerTag -> {
        });

        String result = response.take();
        log.info("Get result from server: {} ", result);
        channel.basicCancel(ctag);

        return result;
    }

    public void validateRequest(PaymentRequest paymentRequest, BindingResult bindingResult) throws RequestException {
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

        //True if checksum success
        if (!checkSumSHA256(paymentRequest, privateKey)) {
            throw new RequestException(ErrorCode.CHECK_SUM_ERROR);
        }

        //True if Token key not exist on day
        if (!checkTokenKey(paymentRequest)) {
            throw new RequestException(ErrorCode.DUPLICATE_TOKEN_KEY);
        }

        //True if real amount <= debit amount
        if (!checkValidAmount(paymentRequest)) {
            log.info("Real amount is invalid (amount > debit amount)");
            throw new RequestException(ErrorCode.INVALID_AMOUNT);
        }
    }

    public boolean checkTokenKey(PaymentRequest paymentRequest) throws RequestException {
        String bankCode = paymentRequest.getBankCode();
        String tokenKey = paymentRequest.getTokenKey();
        log.info("Begin check valid of Token key {}", tokenKey);

        try (Jedis jedis = redisPool.getJedis()) {

            String jsonValue = gson.toJson(paymentRequest);

            boolean checkKeyExist = jedis.exists(tokenKey);
            //get TTL of tokenKey
            long ttl = jedis.ttl(tokenKey);
            log.info("Expire time is: {} seconds", ttl);
            log.info("Key exist? {}", checkKeyExist);

            if (ttl > 0 && checkKeyExist) {
                log.info("Token key exist on day");
                return false;
            }

            log.info("Data put to redis: {}", jsonValue);
            jedis.hset(tokenKey, bankCode, jsonValue);
            jedis.expire(tokenKey, this.getTimeExpire());

            log.info("Token key can use on day");
            return true;
        } catch (RequestException e) {
            log.error("Connect to Redis fail! ");
            throw new RequestException(ErrorCode.CONNECT_REDIS_FAIL);
        }
    }

    public long getTimeExpire() {
        LocalDateTime now1 = LocalDateTime.now();
        LocalDateTime endOfDate = now1.toLocalDate().atTime(LocalTime.MAX);
        return ChronoUnit.SECONDS.between(now1, endOfDate);
    }

    public boolean checkValidAmount(PaymentRequest paymentRequest) {
        return paymentRequest.getRealAmount() < paymentRequest.getDebitAmount();
    }

    public String getPrivateKeyByBankCode(String bankCode) {
        log.info("Begin getPrivateKey() with bank code: {}", bankCode);
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

    public boolean checkValidTimeFormat(String time){

        Date date;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            date = dateFormat.parse(time);
            if(time.equals(dateFormat.format(date))){
                return true;
            }
        }catch (Exception e) {
            log.info("Pay date format is invalid");
            throw new RequestException(ErrorCode.INVALID_DATE_FORMAT);
        }

        return false;
    }
}

