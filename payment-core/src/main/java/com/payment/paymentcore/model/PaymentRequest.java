package com.payment.paymentcore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
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
