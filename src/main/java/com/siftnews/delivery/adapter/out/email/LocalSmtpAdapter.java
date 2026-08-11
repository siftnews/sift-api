package com.siftnews.delivery.adapter.out.email;

import com.siftnews.delivery.application.port.out.SendEmailPort;
import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryFailureCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Profile("!test & !loadtest")
@RequiredArgsConstructor
class LocalSmtpAdapter implements SendEmailPort {

    private final JavaMailSender sender;

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        var message = createMessage(recipient, subject, htmlBody);
        try {
            sender.send(message);
        } catch (Exception exception) {
            throw new DeliveryException(DeliveryFailureCategory.TRANSIENT, exception);
        }
    }

    private jakarta.mail.internet.MimeMessage createMessage(String recipient, String subject, String htmlBody) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            return message;
        } catch (Exception exception) {
            throw new DeliveryException(DeliveryFailureCategory.PERMANENT, exception);
        }
    }
}
