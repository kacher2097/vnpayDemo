package com.example.paymentapi.service.impl;

import com.example.paymentapi.config.ChannelPool;
import com.example.paymentapi.config.RedisPool;
import com.example.paymentapi.config.YAMLConfig;
import com.example.paymentapi.exception.PaymentException;
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

    private static final String RPC_QUEUE = "queue.rpc";
    private final YAMLConfig yamlConfig;
    private final Gson gson;
    private final RedisPool redisPool;
    private final ChannelPool channelPool;

    private final DateTimeUtils dateTimeUtils = DateTimeUtils.getInstance();

    //Constructor Injection
    public PaymentServiceImpl(YAMLConfig yamlConfig, Gson gson, RedisPool redisPool, ChannelPool channelPool) {
        this.yamlConfig = yamlConfig;
        this.gson = gson;
        this.redisPool = redisPool;
        this.channelPool = channelPool;
    }

    @Override
    public ResponseEntity<ResponseObject> sendRequest(PaymentRequest paymentRequest, BindingResult bindingResult,
                                                      String responseId) {
        ErrorCode errorCode = ErrorCode.getInstance();
        log.info("Begin send request with data: {}", paymentRequest);
        String response;
        try {
            log.info("Begin validate request");
            validateRequest(paymentRequest, bindingResult);
        } catch (PaymentException e) {
            log.info("Send request have error code: {}", e.getCode());
            return new MessageResponse().bodyErrorResponse(e.getCode(), errorCode.getDescription(e.getCode()),
                    responseId, "", "");
        }

        try {
            log.info("Request is valid => send message to RabbitMQ");
            response = sendMessage(paymentRequest);
        } catch (PaymentException e) {
            log.error("Got exception when send message {}", e.getMessage());
            return new MessageResponse().bodyErrorResponse(e.getCode(), errorCode.getDescription(e.getCode()),
                    responseId, "", "");
        }

        log.info("Receive message from queue success");
        return new MessageResponse().bodyResponse(responseId, response, "", "");
    }

    public String sendMessage(PaymentRequest paymentRequest) {
        log.info("Begin publish message to queue with data {} ", paymentRequest);
        Channel channel = null;
        String message;
        ConvertUtils convertUtils = ConvertUtils.getInstance();

        try {
            channel = channelPool.getChannel();

//            String rpcQueue = config.readConfigFile().getQueue();
            final String corrId = UUID.randomUUID().toString();

            //TODO tao 1 queue rieng de nhan tin
            String replyQueueName = channel.queueDeclare().getQueue();
            log.info("Create a queue temp {} ", replyQueueName);
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(corrId)
                    .replyTo(replyQueueName)
                    .build();

            log.info("correlationId with message {} ", corrId);
            //Convert payment request to String to publish message
            message = convertUtils.convertObjToJson(paymentRequest);
            log.info("Send message to queue {} with data: {}", RPC_QUEUE, message);
            //Send request to queue
            channel.basicPublish("", RPC_QUEUE, props, message.getBytes(StandardCharsets.UTF_8));

            log.info("End send message to server => wait result response from server");
            return receiveMessage(corrId, replyQueueName, channel);
        } catch (Exception e) {
            log.error("Connect to RabbitMQ fail! {}", e);
            throw new PaymentException(ErrorCode.CONNECT_RABBITMQ_FAIL);
        } finally {
            try {
                if (channel != null) {
                    log.info("Return channel to pool");
                    channelPool.returnChannel(channel);
                }
            } catch (Exception e) {
                log.error("Return channel to pool fail");
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
                throw new PaymentException(ErrorCode.GET_RESPONSE_FAIL);
            }
        };

        String ctag = channel.basicConsume(replyQueueName, false, deliverCallback, consumerTag -> {
        });

        log.info("Ctag {}", ctag);

        String result;
        try {
            result = response.poll(30, TimeUnit.SECONDS);
            log.info("Get result from server: {} ", result);
            channel.basicCancel(ctag);
        } catch (Exception e) {
            log.error("Get result from server is time out");
            throw new PaymentException(ErrorCode.GET_RESULT_TIME_OUT);
        }

        log.info("End Receive message from server success");
        return result;
    }

    public void validateRequest(PaymentRequest paymentRequest, BindingResult bindingResult) {


        if (bindingResult.hasErrors()) {
            log.error("One or more field request is empty or null");
            throw new PaymentException(ErrorCode.NULL_REQUEST);
        }

        String privateKey = getPrivateKeyByBankCode(paymentRequest.getBankCode());
        if (privateKey == null) {
            throw new PaymentException(ErrorCode.NOT_HAVE_PRIVATE_KEY);
        }

        //True if pay date have right format
        if (!dateTimeUtils.isPayDateValid(paymentRequest.getPayDate())) {
            throw new PaymentException(ErrorCode.INVALID_DATE_FORMAT);
        }

        checkValidPromotionCode(paymentRequest);

        if (!checkValidAmount(paymentRequest)) {
            log.info("Real amount is invalid (amount > debit amount)");
            throw new PaymentException(ErrorCode.INVALID_AMOUNT);
        }

        log.info("Check amount success");

        //True if checksum success
        if (!checkSumSHA256(paymentRequest, privateKey)) {
            throw new PaymentException(ErrorCode.CHECK_SUM_ERROR);
        }

        //True if Token key not exist on day
        if (!checkTokenKey(paymentRequest)) {
            throw new PaymentException(ErrorCode.DUPLICATE_TOKEN_KEY);
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

            jedis.setnx(tokenKey, jsonValue);

            jedis.expire(tokenKey, dateTimeUtils.getTimeExpire());

            log.info("End check Token Key success");
            return true;
        } catch (PaymentException e) {
            log.error("Connect to Redis fail! ");
            throw new PaymentException(ErrorCode.CONNECT_REDIS_FAIL);
        }
    }

    public boolean checkValidAmount(PaymentRequest paymentRequest) {
        double realAmount = paymentRequest.getRealAmount();
        double debitAmount = paymentRequest.getDebitAmount();
        log.info("Begin check valid amount with real amount value {} and debit amount value {} ", realAmount, debitAmount);
        return  realAmount <= debitAmount;
    }

    public void checkValidPromotionCode(PaymentRequest paymentRequest) {
        String promotionCode = paymentRequest.getPromotionCode();
        log.info("Begin check valid promotion code with promotionCode {}", promotionCode);
        if (promotionCode == null || promotionCode.isEmpty() || promotionCode.isBlank()) {
            if (paymentRequest.getRealAmount() != paymentRequest.getDebitAmount()) {
                log.info("Real amount different debit amount but promotion code is null, empty or blank => invalid promotion code");
                throw new PaymentException(ErrorCode.INVALID_PROMOTION_CODE);
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
        log.info("Begin getStringToHash with data PaymentRequest {} and private key value [{}]", paymentRequest, privateKey);
        StringBuilder stringBuilder = new StringBuilder();
        log.info("Append payment request and private key to HASH");

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

