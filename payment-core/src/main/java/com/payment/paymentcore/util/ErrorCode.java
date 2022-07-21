package com.payment.paymentcore.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ErrorCode {
    public static final String SEND_BAD_REQUEST = "01";
    public static final String REQUEST_SUCCESS = "00";
    public static final String SYSTEM_MAINTENANCE = "96";
    public static final String DOUBT_TRANSACTION = "08";
    public static final String NULL_RESPONSE = "09";
    public static final String NULL_REQUEST = "03";

    public static final String READ_CONFIG_REDIS_FAIL = "31";
    public static final String READ_CONFIG_RABBITMQ_FAIL = "32";
    public static final String CONNECT_REDIS_FAIL = "51";
    public static final String CONNECT_RABBITMQ_FAIL = "52";
    public static final String CONNECT_DB_FAIL = "53";
    public static final String CODE_EXCEPTION = "54";

    private static final String FILE_CONFIG = "\\config\\error-description.properties";
    public String readErrorDescriptionFile(String code){
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            return properties.getProperty(code);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}

