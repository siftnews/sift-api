package com.siftnews.subscriber.application.service;

import com.siftnews.subscriber.application.port.in.RegisterSubscriberUseCase;
import com.siftnews.subscriber.application.port.in.SubscriberSummary;
import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriberPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.Subscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriberService implements RegisterSubscriberUseCase {

    private final LoadSubscriberPort loadSubscriberPort;
    private final SaveSubscriberPort saveSubscriberPort;

    @Override
    @Transactional
    public SubscriberSummary register(String email, int preferredSendHour) {
        Subscriber subscriber = Subscriber.create(email, preferredSendHour);
        if (loadSubscriberPort.loadByEmail(subscriber.getEmail()).isPresent()) {
            throw new ConflictException("이미 등록된 email입니다.");
        }

        return SubscriberSummary.from(saveSubscriberPort.save(subscriber));
    }
}
