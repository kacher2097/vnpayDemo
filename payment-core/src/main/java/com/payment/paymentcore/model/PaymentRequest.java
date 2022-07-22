package com.payment.paymentcore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private static final long serialVersionUID = 1L;

    private String apiID;
    private String mobile;
    private String bankCode;
    private String accountNo;
    private String payDate;
    private String additionalData;
    private double debitAmount;
    private String respCode;
    private String respDesc;
    private String traceTransfer;
    private String messageType;
    private String checkSum;
    private String orderCode;
    private String userName;
    private double realAmount;
    private String promotionCode;
    private String tokenKey;
    private AddValue addValue;
}
