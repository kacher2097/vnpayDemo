package com.example.paymentapi.service.impl;

import com.example.paymentapi.config.RedisPool;
import com.example.paymentapi.config.YAMLConfig;
import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.Bank;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.service.IPaymentService;
import com.example.paymentapi.util.ErrorCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final YAMLConfig yamlConfig;
    private final Gson gson;

    private final RedisPool redisPool;

    //Constructor Injection
    public PaymentServiceImpl(YAMLConfig yamlConfig, Gson gson, RedisPool redisPool) {
        this.yamlConfig = yamlConfig;
        this.gson = gson;
        this.redisPool = redisPool;
    }

    @Override
    public void validateRequest(PaymentRequest paymentRequest, BindingResult bindingResult) throws RequestException {
        if (bindingResult.hasErrors()) {
            log.error("One or more field request is empty or null");
            throw new RequestException(ErrorCode.NULL_REQUEST);
        }

        String privateKey = getPrivateKeyByBankCode(paymentRequest);
        if (privateKey == null) {
            throw new RequestException(ErrorCode.NOT_HAVE_PRIVATE_KEY);
        }

        //True if checksum success
        if (!checkSumSHA256(paymentRequest, privateKey)) {
            throw new RequestException(ErrorCode.CHECK_SUM_ERROR);
        }

        //True if Token key not exist on day
        if(!checkTokenKey(paymentRequest)){
            throw new RequestException(ErrorCode.DUPLICATE_TOKEN_KEY);
        }

        //True if pay date have right format
        if(!checkValidTimeFormat(paymentRequest.getPayDate())){
            throw new RequestException(ErrorCode.INVALID_DATE_FORMAT);
        }

        //True if real amount <= debit amount
        if(!checkValidAmount(paymentRequest)){
            log.info("Real amount is invalid (amount > debit amount)");
            throw new RequestException(ErrorCode.INVALID_AMOUNT);
        }
    }

    public boolean checkTokenKey(PaymentRequest paymentRequest) throws RequestException{
        String bankCode = paymentRequest.getBankCode();
        String tokenKey = paymentRequest.getTokenKey();
        log.info("Begin check valid of Token key {}", tokenKey);

        try {
            Jedis jedis = redisPool.getJedis();

            String jsonValue = gson.toJson(paymentRequest);

            boolean checkKeyExist = jedis.exists(tokenKey);
            log.info("Key exist? {}", checkKeyExist);

            jedis.hset(tokenKey , bankCode, jsonValue);
            log.info("Data put to redis: {}", jsonValue);
            jedis.expire(tokenKey, this.getTimeExpire());

            long ttl = jedis.ttl(tokenKey);
            log.info("Expire time is: {} seconds", ttl);
            if(ttl > 0 && checkKeyExist){
                log.info("Token key exist on day");
                return false;
            }
            log.info("Token key can use on day");
            return true;
        }catch (RequestException e){
            log.error("Connect to Redis fail! ");
            throw new RequestException(ErrorCode.CONNECT_REDIS_FAIL);
        }
    }

    public long getTimeExpire() {
        LocalDateTime now1 = LocalDateTime.now();
        LocalDateTime endOfDate = now1.toLocalDate().atTime(LocalTime.MAX);
        return ChronoUnit.SECONDS.between(now1, endOfDate);
    }

    public boolean checkValidAmount(PaymentRequest paymentRequest){
        return paymentRequest.getRealAmount() < paymentRequest.getDebitAmount();
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

        log.error("Check sum error cause one or more fields are change");
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

