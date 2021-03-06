package com.example.paymentapi.controller;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import com.example.paymentapi.service.IPaymentService;
import com.example.paymentapi.service.RabbitMQSender;
import com.example.paymentapi.util.Convert;
import com.example.paymentapi.util.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        final String responseId = UUID.randomUUID().toString();
        MessageResponse messageResponse = new MessageResponse();
        try {

            log.info("Begin sendRequest() ");
            ipaymentService.validateRequest(paymentRequest, bindingResult);

            log.info(" Requesting data payment to RabbitMQ {}", paymentRequest);
            String response = rabbitMQSender.call(paymentRequest);

            log.info("Send request success and receive response success with result: {}", response);
            RequestException requestException = Convert.convertJsonMessageToObject2(response);

            return messageResponse.bodyResponse(requestException.getCode(), requestException.getMessage()
                    , responseId, "", "");

        } catch (RequestException e) {
            log.error("Send request fail");
            return messageResponse.bodyResponse(e.getCode(), e.getMessage()
                    , responseId, "", "");

        } catch (IOException | InterruptedException | TimeoutException e) {
            log.error("has ex:",e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    new ResponseObject("1111", e.getMessage(), responseId, "", "")
            );
        }
    }
}
