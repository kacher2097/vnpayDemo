package com.example.paymentapi.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class PaymentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String apiID;

    @NotBlank
    private String mobile;

    @NotBlank
    private String bankCode;

    @NotBlank
    private String accountNo;

    @NotBlank
    private String payDate;

    @NotBlank
    private String additionalData;

    @NotNull
    private double debitAmount;

    @NotBlank
    private String respCode;

    @NotBlank
    private String respDesc;

    @NotBlank
    private String traceTransfer;

    @NotBlank
    private String messageType;

    @NotBlank
    private String checkSum;

    @NotBlank
    private String orderCode;

    @NotBlank
    private String userName;

    @NotNull
    private double realAmount;

    @NotBlank
    private String promotionCode;

    @NotBlank
    private String tokenKey;

    @NotNull
    private AddValue addValue;

}

