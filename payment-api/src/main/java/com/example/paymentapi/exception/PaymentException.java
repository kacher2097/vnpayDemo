package com.example.paymentapi.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentException extends RuntimeException{
    private String code;
    private String message;

    public PaymentException(String code){
        this.code = code;
    }
}
