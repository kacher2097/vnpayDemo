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

    private static DateTimeUtils instance;
    private DateTimeUtils(){}
    public static DateTimeUtils getInstance(){
        if(instance == null){
            instance = new DateTimeUtils();
        }
        return instance;
    }

    public boolean isPayDateValid(String time){
        log.info("Begin check valid of pay date with data {}", time);
        try {
            DateTimeUtils.format.parse(time);
            log.info("End check --- Pay date is valid ");
            return true;
        } catch (Exception e) {
            log.info("End check --- Pay date is invalid format");
            return false;
        }
    }

    public long getTimeExpire() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDate = now.toLocalDate().atTime(LocalTime.MAX);
        return ChronoUnit.SECONDS.between(now, endOfDate);
    }


    public long getTimeExpire(String time) {
        LocalDateTime dateTime = LocalDateTime.parse(time, format);

        //LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDate = dateTime.toLocalDate().atTime(LocalTime.MAX);
        return ChronoUnit.SECONDS.between(dateTime, endOfDate);
    }
}
