package com.example.paymentapi.service.impl;

import com.example.paymentapi.model.Bank;
import com.example.paymentapi.config.RedisPool;
import com.example.paymentapi.config.YAMLConfig;
import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.service.IPaymentService;
import com.example.paymentapi.util.GetTime;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final YAMLConfig yamlConfig;
    private final Gson gson;

    //Constructor Injection
    public PaymentServiceImpl(YAMLConfig yamlConfig, Gson gson) {
        this.yamlConfig = yamlConfig;
        this.gson = gson;
    }

    @Override
    public void validateRequest(PaymentRequest paymentRequest, BindingResult bindingResult) throws RequestException {
        if (bindingResult.hasErrors()) {
            log.error("One or more field request is empty or null");
            throw new RequestException("01", "One or more field request is empty or null");
        }

        String privateKey = getPrivateKeyByBankCode(paymentRequest);
        if (privateKey == null) {
            throw new RequestException("02", "Not have Bank Code in YAML file");
        }

        if (!checkSumSHA256(paymentRequest, privateKey)) {
            throw new RequestException("03", "Check sum error");
        }

        if(!checkTokenKey(paymentRequest)){
            throw new RequestException("015", "Token key is exist in day");
        }

        if(!checkDateRequest(paymentRequest)){
            throw new RequestException("13", "Pay date is invalid");
        }

        if(!checkValidAmount(paymentRequest)){
            throw new RequestException("14", "Real amount is invalid");
        }
    }

    public boolean checkTokenKey(PaymentRequest paymentRequest) throws RequestException{
        RedisPool jedisPool = new RedisPool();
        GetTime getTime = new GetTime();
        log.info("Check Valid of Token key with payment request: {}", paymentRequest);
        String bankCode = paymentRequest.getBankCode();
        String tokenKey = paymentRequest.getTokenKey();
        try {
            Jedis jedis = jedisPool.getJedis();
            String jsonValue = gson.toJson(paymentRequest);

            boolean checkKeyExist = jedis.exists(tokenKey);
            log.info("Key exist? {}", checkKeyExist);

            jedis.hset(tokenKey , bankCode, jsonValue);
            jedis.expire(tokenKey, getTime.getTimeExpire());

            long ttl = jedis.ttl(tokenKey);
            log.info("Expire time is: {} ", ttl);
            if(ttl > 0 && checkKeyExist){
                return false;
            }
            return true;
        }catch (RequestException e){
            log.error("Connect to Redis fail! ");
            throw new RequestException("0032", "Connect to Redis fail");
        }
    }

    public boolean checkValidAmount(PaymentRequest paymentRequest){
        return paymentRequest.getRealAmount() < paymentRequest.getDebitAmount();
    }
    public boolean checkDateRequest(PaymentRequest paymentRequest){
        GetTime getTime = new GetTime();
        return getTime.checkValidTimeFormat(paymentRequest.getPayDate());
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

        log.error("Checksum error");
        return false;
    }


}

