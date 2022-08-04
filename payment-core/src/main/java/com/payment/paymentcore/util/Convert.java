package com.payment.paymentcore.util;

import com.google.gson.Gson;
import com.payment.paymentcore.model.PaymentRequest;

public class Convert {
    private static final Gson gson = GsonUtils.getInstance();

    public static String convertObjToJson(Object clsObj) {
        //convert object to string json
        return gson.toJson(clsObj);
    }

    public static PaymentRequest convertJsonMessageToObject(String message) {
        //Convert json message to Object
        return gson.fromJson(message, PaymentRequest.class);
    }
}
