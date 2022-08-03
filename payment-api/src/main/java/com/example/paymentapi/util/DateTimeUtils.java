package com.example.paymentapi.util;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

    public static long getTimeExpire() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDate = now.toLocalDate().atTime(LocalTime.MAX);
        return ChronoUnit.SECONDS.between(now, endOfDate);
    }


    public long getTimeExpire(String time) {
        LocalDateTime dateTime = LocalDateTime.parse(time, format);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDate = now.toLocalDate().atTime(LocalTime.MAX);
        return ChronoUnit.SECONDS.between(now, endOfDate);
    }
}
