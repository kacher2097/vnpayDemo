package com.example.paymentapi.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AddValue {

    @NotNull
    @NotEmpty
    private String payMethod;

    @NotNull
    private int payMethodMMS;

}
