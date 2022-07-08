package com.example.paymentapi.service.impl;

import com.example.paymentapi.config.Bank;
import com.example.paymentapi.config.RedisPool;
import com.example.paymentapi.config.YAMLConfig;
import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.service.IPaymentService;
import com.example.paymentapi.util.GetTime;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentServiceImpl implements IPaymentService {

    //TODO: xuat file log
    private final YAMLConfig yamlConfig;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Gson gson;

    private static Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    //Constructor Injection
    public PaymentServiceImpl(YAMLConfig yamlConfig, RedisTemplate<String, Object> redisTemplate, Gson gson) {
        this.yamlConfig = yamlConfig;
        this.redisTemplate = redisTemplate;
        this.gson = gson;
    }

    @Override
    public void setDataRequestToRedis(PaymentRequest paymentRequest, BindingResult bindingResult) throws RequestException {
        checkAllValidate(paymentRequest, bindingResult);
        RedisPool jedisPool = new RedisPool();
        try (Jedis jedis = jedisPool.getJedis()) {
            jedis.setex("key", 10000, "payment request");
            // do operations with jedis resource
//            jedis.hset("", "", paymentRequest);
//            @Override
//            public void put(String key, Object value) {
//                String jsonValue = gson.toJson(value);
//                this.jedis.hset(this.cacheName, key, jsonValue);
//            }

        }
        long timeRemaining = ChronoUnit.SECONDS.between( LocalDateTime.now() , LocalDate.now().atTime(LocalTime.MAX));
        //redisTemplate.opsForList().leftPush("key1", paymentRequest);
        log.info(" Set request data to Redis with data: {} ", paymentRequest);
        redisTemplate.opsForHash().put(paymentRequest.getBankCode(), paymentRequest.getTokenKey(), paymentRequest);

        log.debug("Time token key is valid: {}", timeRemaining);
        redisTemplate.expire(paymentRequest.getTokenKey(), timeRemaining, TimeUnit.SECONDS);
        log.info("Data push to redis: {} ", (PaymentRequest) redisTemplate.opsForHash().get(paymentRequest.getBankCode()
                , paymentRequest.getTokenKey()));
//        PaymentRequest paymentRequest1 = (PaymentRequest) redisTemplate.opsForHash().get(paymentRequest.getBankCode(), paymentRequest.getTokenKey());
//        log.info("[{}]", gson.toJson(paymentRequest1));
    }

    public void checkAllValidate(PaymentRequest paymentRequest, BindingResult bindingResult) throws RequestException {
        if (bindingResult.hasErrors()) {
            throw new RequestException("01", "One or more field is empty or null");
        }

        String privateKey = getPrivateKeyByBankCode(paymentRequest);
        if (privateKey == null) {
            throw new RequestException("02", "Not have Bank Code in YAML file");
        }

        if (!checkSumSHA256(paymentRequest, privateKey)) {
            throw new RequestException("03", "Check sum error");
        }

        if(!checkDateRequest(paymentRequest)){
            throw new RequestException("13", "Pay date is invalid");
        }

//        if(!checkValidAmount(paymentRequest)){
//            throw new RequestException("14", "Real amount is invalid");
//        }
    }

    public boolean checkValidAmount(PaymentRequest paymentRequest){
        if(paymentRequest.getRealAmount() > paymentRequest.getDebitAmount()){
            return true;
        }
        return false;
    }
    public boolean checkDateRequest(PaymentRequest paymentRequest){
        GetTime getTime = new GetTime();
        if(getTime.checkValidTimeFormat(paymentRequest.getPayDate())){
            return true;
        }
        return false;
    }

    public String getPrivateKeyByBankCode(PaymentRequest paymentRequest) {
        log.info("Begin getPrivateKey() with Data Payment Request: {}", paymentRequest);
        List<Bank> lstBank = yamlConfig.getAllBanks();
        String bankCode = paymentRequest.getBankCode();
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
        log.info("Append to StringBuilder");
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

        log.error("Checksum error");
        return false;
    }


}

