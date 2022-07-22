package com.payment.paymentcore.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddValue implements Serializable {

    private String payMethod;
    private int payMethodMMS;
}
