package com.example.paymentapi.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class PaymentRequest implements Serializable {

    @NotBlank
    @NotNull
    @NotEmpty
    private String apiID;

    @NotBlank
    @NotNull
    @NotEmpty
    private String mobile;

    @NotBlank
    @NotNull
    @NotEmpty
    private String bankCode;

    @NotBlank
    @NotNull
    @NotEmpty
    private String accountNo;

    @NotBlank
    private String payDate;


    @NotNull
    @NotEmpty
    private String additionalData;

    @NotNull
    private double debitAmount;

    @NotBlank
    @NotNull
    @NotEmpty
    private String respCode;

    @NotBlank
    @NotNull
    @NotEmpty
    private String respDesc;

    @NotBlank
    @NotNull
    @NotEmpty
    private String traceTransfer;

    @NotBlank
    @NotNull
    @NotEmpty
    private String messageType;

    @NotBlank
    @NotNull
    @NotEmpty
    private String checkSum;

    @NotBlank
    @NotNull
    @NotEmpty
    private String orderCode;

    @NotBlank
    @NotNull
    @NotEmpty
    private String userName;

    @NotNull
    private double realAmount;

    private String promotionCode;

    @NotBlank
    @NotNull
    @NotEmpty
    private String tokenKey;

    @NotNull
    private AddValue addValue;

}

