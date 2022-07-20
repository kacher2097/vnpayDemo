package com.example.paymentapi.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class PaymentRequest implements Serializable {

    @NotNull
    @NotEmpty
    private String apiID;

    @NotNull
    @NotEmpty
    private String mobile;

    @NotNull
    @NotEmpty
    private String bankCode;

    @NotNull
    @NotEmpty
    private String accountNo;

    @NotNull
    @NotEmpty
    private String payDate;

    @NotNull
    @NotEmpty
    private String additionalData;

    @NotNull
    private double debitAmount;

    @NotNull
    @NotEmpty
    private String respCode;

    @NotNull
    @NotEmpty
    private String respDesc;

    @NotNull
    @NotEmpty
    private String traceTransfer;

    @NotNull
    @NotEmpty
    private String messageType;

    @NotNull
    @NotEmpty
    private String checkSum;

    @NotNull
    @NotEmpty
    private String orderCode;

    @NotNull
    @NotEmpty
    private String userName;

    @NotNull
    private double realAmount;

    @NotNull
    @NotEmpty
    private String promotionCode;

    @NotNull
    @NotEmpty
    private String tokenKey;

}

