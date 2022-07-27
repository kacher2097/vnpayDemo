package com.example.paymentapi.service.impl;

import com.example.paymentapi.config.Bank;
import com.example.paymentapi.config.RedisConfig2;
import com.example.paymentapi.config.YAMLConfig;
import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.handle.MessageResponse;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import com.example.paymentapi.service.IPaymentService;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PaymentServiceImpl implements IPaymentService {

    private static final Logger log = LogManager.getLogger(PaymentServiceImpl.class);
    private final YAMLConfig yamlConfig;
    private final Gson gson;
    private final RedisConfig2 redis;

    //Constructor Injection
    public PaymentServiceImpl(YAMLConfig yamlConfig, Gson gson, RedisConfig2 jedis) {
        this.yamlConfig = yamlConfig;
        this.gson = gson;
        this.redis = jedis;
    }

    @Override
    public ResponseEntity<ResponseObject> setDataRequestToRedis(PaymentRequest paymentRequest,
                                                                BindingResult bindingResult) {
        try (Jedis jedis = redis.getRedis()){
            checkAllValidate(paymentRequest, bindingResult);
            log.info("Begin setDataToRedis with data {} ", paymentRequest);
            String jsonValue = gson.toJson(paymentRequest);
            try{
                jedis.hset(paymentRequest.getBankCode(), paymentRequest.getTokenKey(), jsonValue);
            } catch (Exception e){
                log.error("Connect to Redis fail");
                throw new RequestException("90", "Connect to Redis fail");
            }

            log.info("Set data to redis success!");
            return MessageResponse.bodyResponse("00", "Send request success");

        } catch (RequestException e){
            return MessageResponse.bodyResponseError(e.getCode(), e.getMessage());
        }

    }

    public void checkAllValidate(PaymentRequest paymentRequest, BindingResult bindingResult) throws RequestException {
        if (bindingResult.hasErrors()) {
            throw new RequestException("01", "One or more field is empty or null");
        }

        String privateKey = getPrivateKeyByBankCode(paymentRequest);
        if (privateKey == null) {
            throw new RequestException("02", "Not have Bank Code in YAML file");
        }

        //True if checksum success
        if (!checkSumSHA256(paymentRequest, privateKey)) {
            throw new RequestException("03", "Checksum error because one or more fields are change");
        }
    }

    public String getPrivateKeyByBankCode(PaymentRequest paymentRequest) {
        log.info("Begin getPrivateKey() with Data Payment Request: {}", paymentRequest);
        List<Bank> lstBank = yamlConfig.getAllBanks();
        String bankCode = paymentRequest.getBankCode();

        for (Bank item : lstBank) {
            if (bankCode.equalsIgnoreCase(item.getBankCode())) {
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
                .hashString(this.getStringToHash(paymentRequest, privateKey).toString(), StandardCharsets.UTF_8)
                .toString();

        log.info(" SHA256 from request: [{}] ", sha256hex);
        if (paymentRequest.getCheckSum().equalsIgnoreCase(sha256hex)) {
            log.info("checkSumSHA256() Checksum success");
            return true;
        }

        log.error("checkSumSHA256() Checksum error because one or more fields are change");
        return false;
    }
}