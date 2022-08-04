package com.example.paymentapi.util;

import com.example.paymentapi.exception.RequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Component
public class ErrorCode {
    public static final String REQUEST_SUCCESS = "00";
    public static final String SEND_BAD_REQUEST = "01";
    public static final String DUPLICATE_TOKEN_KEY = "02";
    public static final String SYSTEM_MAINTENANCE = "96";
    public static final String DOUBT_TRANSACTION = "08";
    public static final String NULL_RESPONSE = "09";
    public static final String NULL_REQUEST = "03";
    public static final String NOT_HAVE_PRIVATE_KEY = "04";
    public static final String INVALID_DATE_FORMAT = "05";
    public static final String INVALID_AMOUNT = "06";
    public static final String CHECK_SUM_ERROR = "07";
    public static final String INVALID_PROMOTION_CODE = "10";
    public static final String READ_CONFIG_RABBITMQ_FAIL = "32";
    public static final String CONNECT_REDIS_FAIL = "33";
    public static final String CONNECT_RABBITMQ_FAIL = "34";
    public static final String GET_RESPONSE_FAIL = "35";
    public static final String CLOSE_REDIS_FAIL = "36";
    public static final String CLOSE_CHANNEL_FAIL = "37";
    public static final String READ_DESCRIPTION_FAIL = "40";
    public static final String CLOSE_DESCRIPTION_FILE_FAIL = "41";
    public static final String CODE_EXCEPTION = "54";
    public static final String GET_RESULT_TIME_OUT = "55";

    private static final String FILE_CONFIG = "\\config\\error-description.properties";

    public String getDescription(String code) {
        log.info("Begin get description error code with code {}", code);
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);

            // load properties from file
            properties.load(inputStream);

            log.info("End get description error code success");
            return properties.getProperty(code);

        } catch (IOException e) {
            log.error("IOException read description error code file {}", e);
            throw new RequestException(ErrorCode.READ_DESCRIPTION_FAIL, "Read description error code file fail");
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    log.info("Close input stream");
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("Close input stream fail");
            }
        }
    }
}

