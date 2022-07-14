package com.example.paymentapi.controller;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import com.example.paymentapi.service.IPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping(path = "/request/send")
public class PaymentController {

    private final IPaymentService ipaymentService;
    //Constructor Injection
    public PaymentController(IPaymentService ipaymentService) {
        this.ipaymentService = ipaymentService;
    }

    @PostMapping
    public ResponseEntity<ResponseObject> sendRequest(@RequestBody @Valid PaymentRequest paymentRequest,
                                                        BindingResult bindingResult) {
        try {
            log.info("Begin sendRequest() ");
            ipaymentService.setDataRequestToRedis(paymentRequest,bindingResult);
            log.info("Send request success");
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("00", "Send request success")
            );

        } catch (RequestException e) {
            log.error("Send request fail with message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ResponseObject(e.getCode(), e.getMessage()));
        }
    }
}
