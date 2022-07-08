package com.example.paymentapi.config;

import lombok.Data;

@Data
public class Bank {
    private String bankCode;
    private String privateKey;
    private String ips;

}
