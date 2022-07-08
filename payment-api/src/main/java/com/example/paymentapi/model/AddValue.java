package com.example.paymentapi.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;


public class AddValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String payMethod;

    private int payMethodMMS;

    public String getPayMethod() {
        return payMethod;
    }

    public int getPayMethodMMS() {
        return payMethodMMS;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = "01";
    }

    public void setPayMethodMMS(int payMethodMMS) {
        this.payMethodMMS = 01;
    }
}
