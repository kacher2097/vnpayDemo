package com.example.paymentapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentApiApplication {
    private static final Logger log = LogManager.getLogger(PaymentApiApplication.class);

    public static void main(String[] args) {
        log.info("Info: {} / {} = {}");
        SpringApplication.run(PaymentApiApplication.class, args);
    }

}
