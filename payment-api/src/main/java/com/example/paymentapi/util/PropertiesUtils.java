package com.example.paymentapi.util;


import java.util.Properties;

public class PropertiesUtils {
    private static Properties instance;

    private PropertiesUtils(){

    }

    public static Properties getInstance(){
        if(instance == null){
            instance = new Properties();
        }
        return instance;
    }
}
