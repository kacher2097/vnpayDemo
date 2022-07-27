package com.example.paymentapi.util;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.ResponseObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Slf4j
public class MessageResponse {

    private ErrorCode errorCode;

    public MessageResponse() {

    }

    public MessageResponse(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ResponseEntity<ResponseObject> bodyResponse(String responseId, String response, String checkSum,
                                                       String addValue) {
        try {
            if (response != null && !response.isEmpty()) {
                log.info("Send request success and receive response success with result: {}", response);
                RequestException requestException = Convert.convertJsonMessageToObject2(response);

                return ResponseEntity.status(HttpStatus.OK).body(
                        new ResponseObject(requestException.getCode(), requestException.getMessage()
                                , responseId, checkSum, addValue));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ResponseObject(ErrorCode.SYSTEM_MAINTENANCE,
                            errorCode.getDescription(ErrorCode.SYSTEM_MAINTENANCE)
                            , responseId, checkSum, addValue)
            );
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ResponseObject(ErrorCode.NULL_RESPONSE, errorCode.getDescription(ErrorCode.NULL_RESPONSE)
                        , responseId, checkSum, addValue)
        );
    }

    public ResponseEntity<ResponseObject> bodyErrorResponse(String code, String message, String responseId,
                                                            String checkSum, String addValue) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ResponseObject(code, message, responseId, checkSum, addValue)
        );
    }

}
