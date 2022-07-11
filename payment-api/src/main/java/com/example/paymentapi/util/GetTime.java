package com.example.paymentapi.util;

import com.example.paymentapi.exception.RequestException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class GetTime {
    public boolean checkValidTimeFormat(String time){

        Date date = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            date = dateFormat.parse(time);
            if(time.equals(dateFormat.format(date))){
                return true;
            }

        }catch (Exception e) {
            throw new RequestException("12", "Format date time invalid");
        }

        return false;
    }

    public long getTimeExpire() {
        LocalDateTime now1 = LocalDateTime.now();
        LocalDateTime endOfDate = now1.toLocalDate().atTime(LocalTime.MAX);
        long seconds = ChronoUnit.SECONDS.between(now1, endOfDate);
        return seconds;
    }
}
