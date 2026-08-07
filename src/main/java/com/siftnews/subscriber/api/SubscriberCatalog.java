package com.siftnews.subscriber.api;

import java.util.List;

public interface SubscriberCatalog {

    List<DeliveryRecipient> findActiveRecipients(Long topicId, int preferredSendHour);
}
