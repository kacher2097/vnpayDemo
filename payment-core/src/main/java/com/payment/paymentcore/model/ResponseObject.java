package com.payment.paymentcore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseObject {
    private String code;
    private String message;
    private int responseId;
    private String checkSum;
    private String addValue;

}
