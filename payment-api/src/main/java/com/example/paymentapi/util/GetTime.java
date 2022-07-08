package com.example.paymentapi.util;

import com.example.paymentapi.exception.RequestException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public long getTimeExpire(String dateStart, String dateEnd){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date d1 = null;
        Date d2 = null;

        try {
            dateFormat.format(d1);
            d1 = dateFormat.parse(dateStart);
            d2 = dateFormat.parse(dateEnd);

        } catch (ParseException e) {

        }
        // Get msec from each, and subtract.
        long diff = d2.getTime() - d1.getTime();
        long diffHours = diff / (60 * 60 * 1000);

        //SO giay
        long diffSeconds = diff / 1000;
        System.out.println("Số giờ: " + diffHours + " hours.");
        return diffHours;


//        long diffMinutes = diff / (60 * 1000);


//        System.out.println("Số giây : " + diffSeconds + " seconds.");
//        System.out.println("Số phút: " + diffMinutes + " minutes.");

    }

//    public static void main(String[] args) {
//
//        String dateStart = "20220627093358";
//        String dateStop = "20220627235959";
//        String dateValid = "20220627235959";
//        // Custom date format
//        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
//
//        Date date = null;
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
//            date = sdf.parse(dateValid);
//            if (!dateStop.equals(sdf.format(date))) {
//                date = null;
//            }
//        } catch (ParseException ex) {
//            ex.printStackTrace();
//        }
//
//    }
//        Date d1 = null;
//        Date d2 = null;
//
//        try {
//            d1 = format.parse(dateStart);
//
//            d2 = format.parse(dateStop);
//
//        } catch (ParseException e) {
//
//        }
//        // Get msec from each, and subtract.
//        long diff = d2.getTime() - d1.getTime();
//        long diffSeconds = diff / 1000;
//        long diffMinutes = diff / (60 * 1000);
//        long diffHours = diff / (60 * 60 * 1000);
//
//        System.out.println("Số giây : " + diffSeconds + " seconds.");
//        System.out.println("Số phút: " + diffMinutes + " minutes.");
//        System.out.println("Số giờ: " + diffHours + " hours.");
//    }
}
