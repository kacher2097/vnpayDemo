package com.example.paymentapi.config;

import com.example.paymentapi.model.Bank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("banks")
public class YAMLConfig {

    private List<Bank> allBanks = new ArrayList<>();

    public List<Bank> getAllBanks() {
        return allBanks;
    }

    public void setAllBanks(List<Bank> allBanks) {
        this.allBanks = allBanks;
    }
}
