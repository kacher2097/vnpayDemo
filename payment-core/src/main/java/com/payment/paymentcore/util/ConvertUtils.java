package com.payment.paymentcore.util;

import com.google.gson.Gson;
import com.payment.paymentcore.model.PaymentRequest;

public class ConvertUtils {
    private static final Gson gson = GsonUtils.getInstance();

    private static ConvertUtils instance;

    public static ConvertUtils getInstance() {
        if (instance == null) {
            instance = new ConvertUtils();
        }
        return instance;
    }

    public String convertObjToJson(Object clsObj) {
        //convert object to string json
        return gson.toJson(clsObj);
    }

    public PaymentRequest convertJsonToObj(String message) {
        //Convert json message to Object
        return gson.fromJson(message, PaymentRequest.class);
    }
}
