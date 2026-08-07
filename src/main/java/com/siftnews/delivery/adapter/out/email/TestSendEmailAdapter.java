package com.siftnews.delivery.adapter.out.email;

import com.siftnews.delivery.application.port.out.SendEmailPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
class TestSendEmailAdapter implements SendEmailPort {

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        // 테스트 프로필에서는 외부 SMTP 전송을 금지한다.
    }
}
