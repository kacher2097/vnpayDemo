package com.payment.paymentcore.service;

import com.google.gson.Gson;
import com.payment.paymentcore.model.PaymentRequest;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public class PaymentService {
    private static final String URI = "https://api.foodbook.vn/ipos/ws/xpartner/callback/vnpay";

    public PaymentRequest convertJsonMessageToObject(String message){
        Gson gson = new Gson();
        PaymentRequest paymentRequest = gson.fromJson(message, PaymentRequest.class);

        return paymentRequest;
    }

    public WebTarget getWebTarget(){
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);

        return client.target(URI);
    }

    public int sendApi(PaymentRequest paymentRequest){
        WebTarget webTarget = getWebTarget();
        Response response = webTarget.request().post(
                Entity.entity(paymentRequest, MediaType.APPLICATION_JSON),
                Response.class);

        return response.getStatus();
    }

}
