package com.siftnews.delivery.adapter.out.email;

import com.siftnews.delivery.application.port.out.SendEmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
class LocalSmtpAdapter implements SendEmailPort {

    private final JavaMailSender sender;

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            sender.send(message);
        } catch (Exception exception) {
            throw new IllegalStateException("메일 발송에 실패했습니다.", exception);
        }
    }
}
