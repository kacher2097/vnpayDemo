package com.example.paymentapi.util;

import com.google.gson.Gson;

public class Convert {
    public static String convertObjToString(Object clsObj) {
        //convert object  to string json
        Gson gson = new Gson();
        String jsonSender = new Gson().toJson(clsObj);
        return jsonSender;
    }
}
