package com.siftnews.subscriber.application.service;

import com.siftnews.content.api.TopicCatalog;
import com.siftnews.subscriber.application.port.in.ManageSubscriptionUseCase;
import com.siftnews.subscriber.application.port.in.SubscriptionSummary;
import com.siftnews.subscriber.application.port.out.LoadSubscriberPort;
import com.siftnews.subscriber.application.port.out.LoadSubscriptionPort;
import com.siftnews.subscriber.application.port.out.SaveSubscriptionPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.SubscriberNotFoundException;
import com.siftnews.subscriber.domain.Subscription;
import com.siftnews.subscriber.domain.SubscriptionNotFoundException;
import com.siftnews.subscriber.domain.TopicNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements ManageSubscriptionUseCase {

    private final LoadSubscriberPort loadSubscriberPort;
    private final LoadSubscriptionPort loadSubscriptionPort;
    private final SaveSubscriptionPort saveSubscriptionPort;
    private final TopicCatalog topicCatalog;

    @Override
    @Transactional
    public SubscriptionSummary subscribe(Long subscriberId, Long topicId) {
        ensureSubscriber(subscriberId);
        ensureTopic(topicId);

        var existing = loadSubscriptionPort.load(subscriberId, topicId);
        if (existing.isEmpty()) {
            return SubscriptionSummary.from(
                    saveSubscriptionPort.save(Subscription.create(subscriberId, topicId)));
        }

        if (existing.get().getStatus().isActive()) {
            throw new ConflictException("이미 존재하는 구독입니다.");
        }

        return SubscriptionSummary.from(saveSubscriptionPort.save(existing.get().reactivate()));
    }

    @Override
    @Transactional
    public void unsubscribe(Long subscriberId, Long topicId) {
        ensureSubscriber(subscriberId);
        Subscription subscription = loadSubscriptionPort.load(subscriberId, topicId)
                .orElseThrow(() -> new SubscriptionNotFoundException("구독을 찾을 수 없습니다."));
        saveSubscriptionPort.save(subscription.unsubscribe());
    }

    private void ensureSubscriber(Long subscriberId) {
        if (loadSubscriberPort.load(subscriberId).isEmpty()) {
            throw new SubscriberNotFoundException("구독자를 찾을 수 없습니다.");
        }
    }

    private void ensureTopic(Long topicId) {
        if (topicCatalog.findActive(topicId).isEmpty()) {
            throw new TopicNotFoundException("활성 토픽을 찾을 수 없습니다.");
        }
    }
}
