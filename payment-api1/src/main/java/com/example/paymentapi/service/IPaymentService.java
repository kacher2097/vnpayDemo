package com.example.paymentapi.service;

import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

public interface IPaymentService {
    ResponseEntity<ResponseObject> setDataRequestToRedis(PaymentRequest paymentRequest, BindingResult bindingResult);
}
