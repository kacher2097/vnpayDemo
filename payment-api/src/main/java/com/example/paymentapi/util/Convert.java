package com.example.paymentapi.util;

import com.example.paymentapi.exception.RequestException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.IOException;

public class Convert {
    public static String convertObjToString(Object clsObj) {
        //convert object  to string json
        return new Gson().toJson(clsObj);
    }

    //TODO su dung chung thu vien nhan gui giong nhau
    public static RequestException convertJsonMessageToObject2(String messageResponse) throws IOException {
        if (messageResponse == null) {
            return new RequestException("93", "Message response is null");
        }

        //TODO han che khoi tao moi
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.readValue(messageResponse, RequestException.class);
        JsonNode jsonNodeRoot = objectMapper.readTree(messageResponse);
        JsonNode jsonCode = jsonNodeRoot.get("code");
        JsonNode jsonMessage = jsonNodeRoot.get("message");

        String code = jsonCode.asText();
        String message = jsonMessage.asText();

        return new RequestException(code, message);

    }
}
