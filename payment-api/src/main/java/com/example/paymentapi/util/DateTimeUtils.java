package com.example.paymentapi.util;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;

@Slf4j
public class DateTimeUtils {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    public static boolean isPayDateValid(String time){
        try {
            DateTimeUtils.dateFormat.parse(time);
            return true;
        } catch (Exception e) {
            log.info("Pay date have invalid format");
            return false;
        }
    }
}
