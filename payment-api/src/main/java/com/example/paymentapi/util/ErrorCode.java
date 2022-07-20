package com.example.paymentapi.util;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
public class ErrorCode {
    public static final String SEND_BAD_REQUEST = "01";
    public static final String REQUEST_SUCCESS = "00";
    public static final String DUPLICATE_TOKEN_KEY = "02";
    public static final String SYSTEM_MAINTENANCE = "96";
    public static final String DOUBT_TRANSACTION = "08";
    public static final String NULL_RESPONSE = "09";
    public static final String NULL_REQUEST = "03";
    public static final String NOT_HAVE_PRIVATE_KEY = "04";
    public static final String INVALID_DATE_FORMAT = "05";
    public static final String INVALID_AMOUNT = "06";


    private static final String FILE_CONFIG = "\\config\\error-description.properties";
    public String readErrorDescriptionFile(String code){
        Properties properties = new Properties();
        InputStream inputStream = null;
        String errorDescription;
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            // get property by name
            errorDescription = properties.getProperty(code);
            return errorDescription;

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

