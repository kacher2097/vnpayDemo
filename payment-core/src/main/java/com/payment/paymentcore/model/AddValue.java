package com.payment.paymentcore.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class AddValue implements Serializable {

    private static final long serialVersionUID = 1L;
    @NotBlank
    private String payMethod;
    @NotBlank
    private int payMethodMMS;
}
