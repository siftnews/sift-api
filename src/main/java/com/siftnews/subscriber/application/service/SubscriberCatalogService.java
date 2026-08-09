package com.siftnews.subscriber.application.service;

import com.siftnews.subscriber.api.DeliveryRecipient;
import com.siftnews.subscriber.api.SubscriberCatalog;
import com.siftnews.subscriber.application.port.out.LoadTopicSubscribersPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class SubscriberCatalogService implements SubscriberCatalog {

    private final LoadTopicSubscribersPort loadTopicSubscribersPort;

    @Override
    public List<DeliveryRecipient> findActiveRecipients(Long topicId, int preferredSendHour) {
        return loadTopicSubscribersPort.loadActive(topicId, preferredSendHour);
    }
}
