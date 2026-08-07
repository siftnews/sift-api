package com.siftnews.subscriber.application.port.out;

import com.siftnews.subscriber.api.DeliveryRecipient;

import java.util.List;

public interface LoadTopicSubscribersPort {

    List<DeliveryRecipient> loadActive(Long topicId, int preferredSendHour);
}
