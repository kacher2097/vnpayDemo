package com.example.paymentapi.service.impl;

import com.example.paymentapi.config.Bank;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PaymentServiceImpl implements IPaymentService {

    private final YAMLConfig yamlConfig;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Gson gson;

    private static final Logger log = LogManager.getLogger(PaymentServiceImpl.class);

    //Constructor Injection
    public PaymentServiceImpl(YAMLConfig yamlConfig, RedisTemplate<String, Object> redisTemplate, Gson gson) {
        this.yamlConfig = yamlConfig;
        this.redisTemplate = redisTemplate;
        this.gson = gson;
    }

    @Override
    public ResponseEntity<ResponseObject> setDataRequestToRedis(PaymentRequest paymentRequest, BindingResult bindingResult) {

        try {
            checkAllValidate(paymentRequest, bindingResult);
            log.info(" Hset to Redis with data: {} ", paymentRequest);
            redisTemplate.opsForHash().put(paymentRequest.getBankCode(), paymentRequest.getTokenKey(), paymentRequest);

            PaymentRequest paymentRequest1 = (PaymentRequest) redisTemplate.opsForHash().
                    get(paymentRequest.getBankCode(), paymentRequest.getTokenKey());
            log.info("Check data put into Redis: [{}]", gson.toJson(paymentRequest1));

            return MessageResponse.bodyResponse("00", "Send request success");

        } catch (RequestException e){
            return MessageResponse.bodyResponseError(e.getCode(), e.getMessage());
        }

//        if (!checkAllValidate(paymentRequest, bindingResult)) {
//            return MessageResponse.bodyResponseError("", "");
//        } else {
//            log.info(" Hset to Redis with data: {} ", paymentRequest);
//            redisTemplate.opsForHash().put(paymentRequest.getBankCode(), paymentRequest.getTokenKey(), paymentRequest);
//
//            PaymentRequest paymentRequest1 = (PaymentRequest) redisTemplate.opsForHash().
//                    get(paymentRequest.getBankCode(), paymentRequest.getTokenKey());
//            log.info("Check data put into Redis: [{}]", gson.toJson(paymentRequest1));
//
//
//            return MessageResponse.bodyResponse("", "");
//        }

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

        //TODO thay doi dung for bang case
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

    /**
     * @param paymentRequest
     * @param privateKey
     * @return true if valid
     */
    public boolean checkSumSHA256(PaymentRequest paymentRequest, String privateKey) {
        log.info(" Begin checkSumSHA256() with data payment request {}", paymentRequest);

        //FIXED
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

