package com.example.paymentapi.util;

import com.example.paymentapi.model.ResponseObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class MessageResponse {
    public  ResponseEntity<ResponseObject> bodyResponse(String code, String message, String responseId,
                                                          String checkSum, String addValue){
        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject(code, message, responseId, checkSum, addValue)
        );

    }


}
