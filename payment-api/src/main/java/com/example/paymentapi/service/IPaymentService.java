package com.example.paymentapi.service;

import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface IPaymentService {
    void validateRequest(PaymentRequest paymentRequest, BindingResult bindingResult);

    ResponseEntity<ResponseObject> sendRequest(PaymentRequest paymentRequest, BindingResult bindingResult,
                                               String responseId);
}
