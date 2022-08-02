package com.payment.paymentcore.service;

import com.payment.paymentcore.model.PaymentRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class PaymentService {
    private static final Logger log = LogManager.getLogger(PaymentService.class);
    private static final String URI = "https://api.foodbook.vn/ipos/ws/xpartner/callback/vnpay";

    public WebTarget getWebTarget() {
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        log.info("Web target send request {}", client.target(URI));
        return client.target(URI);
    }

    public int sendApi(PaymentRequest paymentRequest) {

        WebTarget webTarget = getWebTarget();
        Response response = webTarget.request().post(
                Entity.entity(paymentRequest, MediaType.APPLICATION_JSON));

        return response.getStatus();
    }

}
