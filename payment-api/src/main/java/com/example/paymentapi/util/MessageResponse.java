package com.example.paymentapi.util;

import com.example.paymentapi.exception.PaymentException;
import com.example.paymentapi.model.ResponseObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Slf4j
public class MessageResponse {

    public MessageResponse() {

    }

    public ResponseEntity<ResponseObject> bodyResponse(String responseId, String response, String checkSum,
                                                       String addValue) {
        ErrorCode errorCode = ErrorCode.getInstance();
        ConvertUtils convertUtils = ConvertUtils.getInstance();
        try {
            if (response != null && !response.isEmpty()) {
                log.info("Send request and receive response success with result: {}", response);
                PaymentException paymentException = convertUtils.convertJsonToObj(response);

                log.info("----- End request success -----");
                return ResponseEntity.status(HttpStatus.OK).body(
                        new ResponseObject(paymentException.getCode(), paymentException.getMessage()
                                , responseId, checkSum, addValue));
            } else {
                log.info("------ End send request ------ Response from server is null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ResponseObject(ErrorCode.NULL_RESPONSE, errorCode.getDescription(ErrorCode.NULL_RESPONSE)
                                , responseId, checkSum, addValue)
                );
            }
        } catch (Exception e) {
            log.info("------ End send request ------ The system is maintenance");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ResponseObject(ErrorCode.SYSTEM_MAINTENANCE,
                            errorCode.getDescription(ErrorCode.SYSTEM_MAINTENANCE)
                            , responseId, checkSum, addValue)
            );
        }
    }

    public ResponseEntity<ResponseObject> bodyErrorResponse(String code, String message, String responseId,
                                                            String checkSum, String addValue) {
        log.info("------ End send request result fail -------");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ResponseObject(code, message, responseId, checkSum, addValue)
        );
    }

}
