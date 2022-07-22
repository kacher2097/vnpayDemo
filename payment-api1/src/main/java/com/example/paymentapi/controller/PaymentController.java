package com.example.paymentapi.controller;

import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import com.example.paymentapi.service.IPaymentService;
import lombok.extern.slf4j.Slf4j;
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
//        try {
//            log.info("Begin sendRequest() with data {}", paymentRequest);
            return ipaymentService.setDataRequestToRedis(paymentRequest, bindingResult);
            //log.info("Send request success with code: {}", "00");
//            return new MessageResponse().bodyResponse("00", "Send request and put data to Redis success");
//
//
//        } catch (RequestException e) {
//            log.error("Send request fail with code: {}", e.getCode());
//            return new MessageResponse().bodyResponse(e.getCode(), e.getMessage());
//        }
    }
}
