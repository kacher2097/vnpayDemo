package com.example.paymentapi.util;

import com.example.paymentapi.exception.PaymentException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
@Slf4j
public class ConvertUtils {

    private static ConvertUtils instance;

    public static ConvertUtils getInstance(){
        if(instance == null){
            instance = new ConvertUtils();
        }
        return instance;
    }

    private ConvertUtils(){

    }

    public String convertObjToJson(Object clsObj) {
        //convert object  to string json
        log.info("Return Object to Json");
        return new Gson().toJson(clsObj);
    }

    //TODO su dung chung thu vien nhan gui giong nhau
    public PaymentException convertJsonToObj(String messageResponse) throws IOException {
        //TODO han che khoi tao moi
        ObjectMapper objectMapper = new ObjectMapper();
        log.info("Begin convert message response to object, message data {} ", messageResponse);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.readValue(messageResponse, PaymentException.class);
        JsonNode jsonNodeRoot = objectMapper.readTree(messageResponse);
        JsonNode jsonCode = jsonNodeRoot.get("code");
        JsonNode jsonMessage = jsonNodeRoot.get("message");

        String code = jsonCode.asText();
        String message = jsonMessage.asText();
        log.info("End convert and get code [{}], message [{}]", code, message);
        return new PaymentException(code, message);

    }
}
