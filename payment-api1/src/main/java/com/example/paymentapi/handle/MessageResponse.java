package com.example.paymentapi.handle;

import com.example.paymentapi.model.ResponseObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class MessageResponse {

    public static ResponseEntity<ResponseObject> bodyResponse(String code, String message){
        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject(code, message)
        );
    }

    public static ResponseEntity<ResponseObject> bodyResponseError(String code, String message){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ResponseObject(code, message)
        );
    }
}
