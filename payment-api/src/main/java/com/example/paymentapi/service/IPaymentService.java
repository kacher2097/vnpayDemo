package com.example.paymentapi.service;

import com.example.paymentapi.model.PaymentRequest;
import org.springframework.validation.BindingResult;

public interface IPaymentService {
    void setDataRequestToRedis(PaymentRequest paymentRequest,BindingResult bindingResult);
}
