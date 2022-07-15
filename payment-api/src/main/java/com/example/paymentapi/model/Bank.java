package com.example.paymentapi.model;

import lombok.Data;

@Data
public class Bank {
    private String bankCode;
    private String privateKey;
    private String ips;

}
