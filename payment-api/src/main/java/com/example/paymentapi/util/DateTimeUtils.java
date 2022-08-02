package com.example.paymentapi.util;
import lombok.extern.slf4j.Slf4j;
import java.time.format.DateTimeFormatter;

@Slf4j
public class DateTimeUtils {
//    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static boolean isPayDateValid(String time){
        try {
            DateTimeUtils.format.parse(time);
            return true;
        } catch (Exception e) {
            log.info("Pay date have invalid format");
            return false;
        }
    }
}
