package com.example.paymentapi.util;

import com.example.paymentapi.exception.RequestException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import com.google.gson.Gson;

import java.io.IOException;

public class Convert {
    public static String convertObjToString(Object clsObj) {
        //convert object  to string json
        Gson gson = new Gson();
        String jsonSender = new Gson().toJson(clsObj);
        return jsonSender;
    }

    public static RequestException convertJsonMessageToObject(String message){
        Gson gson = new Gson();
        RequestException paymentRequest = gson.fromJson(message, RequestException.class);

        return paymentRequest;
    }

    public static RequestException convertJsonMessageToObject2(String messageResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RequestException requestException = objectMapper.readValue(messageResponse, RequestException.class); //
        JsonNode jsonNodeRoot = objectMapper.readTree(messageResponse);
        JsonNode jsonCode = jsonNodeRoot.get("code");
        JsonNode jsonMessage = jsonNodeRoot.get("message");

        String code = jsonCode.asText();
        String message = jsonMessage.asText();

        return new RequestException(code, message);
    }
}
