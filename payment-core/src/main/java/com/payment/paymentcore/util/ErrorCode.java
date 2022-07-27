package com.payment.paymentcore.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ErrorCode {

    public static ErrorCode instance;

    public static ErrorCode getInstance() {
        if (instance == null) {
            instance = new ErrorCode();
        }
        return instance;
    }

    public static final String SEND_BAD_REQUEST = "01";
    public static final String REQUEST_SUCCESS = "00";
    public static final String SYSTEM_MAINTENANCE = "96";
    public static final String DOUBT_TRANSACTION = "08";
    public static final String NULL_RESPONSE = "09";
    public static final String READ_CONFIG_RABBITMQ_FAIL = "30";
    public static final String CONNECT_RABBITMQ_FAIL = "31";
    public static final String CHANNEL_RABBITMQ_TIMEOUT = "32";
    public static final String AWAKE_CONNECT_RABBITMQ_FAIL = "33";

    public static final String CREATE_CHANNEL_RABBITMQ_FAIL = "35";
    public static final String READ_DESCRIPTION_FAIL = "40";

    public static final String CONNECT_DB_FAIL = "53";
    public static final String SQL_EXCEPTION = "55";
    public static final String INSERT_INTO_DB_FAIL = "56";

    private static final String FILE_CONFIG = "\\config\\error-description.properties";

    public String readErrorDescriptionFile(String code) {
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            return properties.getProperty(code);

        } catch (IOException e) {
            throw new PaymentException(ErrorCode.READ_DESCRIPTION_FAIL, "Read error code description file fail");

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
    }
}

