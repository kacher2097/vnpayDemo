package com.example.paymentapi.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RequestException extends RuntimeException{
    private String code;
    private String message;

}
