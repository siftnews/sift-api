package com.siftnews.delivery.adapter.out.email;

import com.siftnews.delivery.application.port.out.SendEmailPort;
import com.siftnews.delivery.domain.DeliveryException;
import com.siftnews.delivery.domain.DeliveryFailureCategory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * loadtest 전용 발송 sink — SMTP·수신자·본문을 사용하지 않고 처리 비용만 측정한다.
 * <p>
 * 실패 주입과 지연은 동일한 호출 순서에서 재현할 수 있는 설정값으로만 제어한다. 실제 메일
 * 내용은 어떤 필드에도 저장하지 않는다.
 */
@Component
@Profile("loadtest & !test")
class LoadtestSendEmailAdapter implements SendEmailPort {

    private final MeterRegistry meterRegistry;
    private final Counter sentCounter;
    private final Counter failedCounter;
    private final int failureEvery;
    private final long processingDelayMillis;
    private final AtomicLong sendSequence = new AtomicLong();

    LoadtestSendEmailAdapter(
            MeterRegistry meterRegistry,
            @Value("${sift.load-test.mail.failure-every:0}") int failureEvery,
            @Value("${sift.load-test.mail.processing-delay-ms:0}") long processingDelayMillis
    ) {
        if (failureEvery < 0) {
            throw new IllegalArgumentException("loadtest mail failure-every는 0 이상이어야 합니다: "
                    + failureEvery);
        }
        if (processingDelayMillis < 0) {
            throw new IllegalArgumentException("loadtest mail processing-delay-ms는 0 이상이어야 합니다: "
                    + processingDelayMillis);
        }
        this.meterRegistry = meterRegistry;
        this.sentCounter = meterRegistry.counter("sift.delivery.loadtest.mail.sent");
        this.failedCounter = meterRegistry.counter("sift.delivery.loadtest.mail.failed");
        this.failureEvery = failureEvery;
        this.processingDelayMillis = processingDelayMillis;
    }

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String status = "sent";
        long sequence = sendSequence.incrementAndGet();
        try {
            if (failureEvery > 0 && sequence % failureEvery == 0) {
                status = "failed";
                throw new DeliveryException(DeliveryFailureCategory.TRANSIENT,
                        new IllegalStateException("loadtest failure injection"));
            }
            if (processingDelayMillis > 0) {
                LockSupport.parkNanos(processingDelayMillis * 1_000_000L);
            }
            sentCounter.increment();
        } catch (DeliveryException exception) {
            failedCounter.increment();
            throw exception;
        } finally {
            sample.stop(Timer.builder("sift.delivery.loadtest.mail.duration")
                    .tag("status", status)
                    .register(meterRegistry));
        }
    }
}
