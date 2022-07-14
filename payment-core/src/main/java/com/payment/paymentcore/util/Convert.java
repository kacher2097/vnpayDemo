package com.payment.paymentcore.util;

import com.google.gson.Gson;
import com.payment.paymentcore.model.PaymentRequest;

public class Convert {
    public static String convertObjToString(Object clsObj) {
        //convert object  to string json
        Gson gson = new Gson();
        String jsonSender = new Gson().toJson(clsObj);
        return jsonSender;
    }

    public static PaymentRequest convertJsonMessageToObject(String message){
        Gson gson = new Gson();
        PaymentRequest paymentRequest = gson.fromJson(message, PaymentRequest.class);

        return paymentRequest;
    }
}
