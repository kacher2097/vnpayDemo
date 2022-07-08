package com.example.paymentapi.controller;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import com.example.paymentapi.service.IPaymentService;
import com.example.paymentapi.service.RabbitMQSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping(path = "/request/send")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final IPaymentService ipaymentService;
    private final RabbitMQSender rabbitMQSender;

    //Constructor Injection
    public PaymentController(IPaymentService ipaymentService, RabbitMQSender rabbitMQSender) {
        this.ipaymentService = ipaymentService;
        this.rabbitMQSender = rabbitMQSender;
    }

    @PostMapping
    public ResponseEntity<ResponseObject> sendRequest(@RequestBody @Valid PaymentRequest paymentRequest,
                                                        BindingResult bindingResult) {
        try {
            final String responseId = UUID.randomUUID().toString();
            log.info("Begin sendRequest() ");
            ipaymentService.setDataRequestToRedis(paymentRequest, bindingResult);

            log.info(" Requesting data payment {}", paymentRequest);
            String response = rabbitMQSender.call(paymentRequest);

            log.info(" Response from server: {}" , response );

            log.info("Send request success");
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("00", "Send request success")
            );

        } catch (RequestException e) {
            log.error("Send request fail");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ResponseObject(e.getCode(), e.getMessage()));
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}