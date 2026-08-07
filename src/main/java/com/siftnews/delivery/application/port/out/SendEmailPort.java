package com.siftnews.delivery.application.port.out;

public interface SendEmailPort {

    void send(String recipient, String subject, String htmlBody);
}
