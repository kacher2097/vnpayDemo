package com.example.paymentapi.controller;

import com.example.paymentapi.exception.RequestException;
import com.example.paymentapi.model.PaymentRequest;
import com.example.paymentapi.model.ResponseObject;
import com.example.paymentapi.service.IPaymentService;
import com.example.paymentapi.service.RabbitMQSender;
import com.example.paymentapi.util.ErrorCode;
import com.example.paymentapi.util.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ErrorCode errorCode;

    //Constructor Injection
    public PaymentController(IPaymentService ipaymentService, RabbitMQSender rabbitMQSender, ErrorCode errorCode) {
        this.ipaymentService = ipaymentService;
        this.rabbitMQSender = rabbitMQSender;
        this.errorCode = errorCode;
    }

    @PostMapping
    public ResponseEntity<ResponseObject> sendRequest(@RequestBody @Valid PaymentRequest paymentRequest,
                                                      BindingResult bindingResult) {
        final String responseId = UUID.randomUUID().toString();
        log.info("Begin send request with data: {}", paymentRequest);
        try {

            log.info("Validate request with data {} ", paymentRequest);
            ipaymentService.validateRequest(paymentRequest, bindingResult);

            log.info(" Requesting data payment to RabbitMQ {}", paymentRequest);
            String response = rabbitMQSender.call(paymentRequest);

            return new MessageResponse().bodyResponse(responseId, response);

        } catch (RequestException e) {
            log.error("Send request fail with error code: {}", e.getCode());
            return new MessageResponse().bodyErrorResponse(e.getCode(),
                    errorCode.readErrorDescriptionFile(e.getCode())
                    , responseId, "", "");

        } catch (IOException | InterruptedException | TimeoutException e) {
            log.error("Code have exception {} :", e.toString());
            return new MessageResponse(errorCode).bodyErrorResponse(ErrorCode.CODE_EXCEPTION,
                    errorCode.readErrorDescriptionFile(ErrorCode.CODE_EXCEPTION) + e.getMessage()
                    , responseId, "", "");
        }
    }
}
