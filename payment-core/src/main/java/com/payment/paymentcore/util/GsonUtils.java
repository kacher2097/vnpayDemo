package com.payment.paymentcore.util;

import com.google.gson.Gson;

public class GsonUtils {
    private static Gson instance;

    public static Gson getInstance(){
        if(instance == null){
            instance = new Gson();
        }
        return instance;
    }

    private GsonUtils(){

    }
}
