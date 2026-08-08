package com.siftnews.delivery.adapter.out.email;

import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryFailureCategory;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalSmtpAdapterTest {

    @Test
    void wrapsSmtpFailureWithoutPersistingRawProviderDetails() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl() {
            @Override
            public void send(jakarta.mail.internet.MimeMessage mimeMessage) {
                throw new IllegalStateException("raw SMTP response for reader@example.com");
            }
        };
        LocalSmtpAdapter adapter = new LocalSmtpAdapter(sender);

        assertThatThrownBy(() -> adapter.send("reader@example.com", "Sift", "<p>Hello</p>"))
                .isInstanceOfSatisfying(DeliveryException.class, exception -> {
                    assertThat(exception.getCategory()).isEqualTo(DeliveryFailureCategory.TRANSIENT);
                    assertThat(exception.getMessage()).doesNotContain("reader@example.com", "raw SMTP response");
                });
    }
}
