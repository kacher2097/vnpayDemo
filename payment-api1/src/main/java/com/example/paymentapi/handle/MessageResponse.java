package com.example.paymentapi.handle;

import com.example.paymentapi.model.ResponseObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class MessageResponse {

    public ResponseEntity<ResponseObject> bodyResponse(String code, String message){
        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject(code, message)
        );

    }
}
