package com.siftnews.subscriber.adapter.in.web;

import com.siftnews.subscriber.adapter.in.web.dto.RegisterSubscriberRequest;
import com.siftnews.subscriber.adapter.in.web.dto.SubscribeRequest;
import com.siftnews.subscriber.application.port.in.ManageSubscriptionUseCase;
import com.siftnews.subscriber.application.port.in.RegisterSubscriberUseCase;
import com.siftnews.subscriber.application.port.in.SubscriberSummary;
import com.siftnews.subscriber.application.port.in.SubscriptionSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/subscribers")
@RequiredArgsConstructor
public class SubscriberController {

    private final RegisterSubscriberUseCase registerSubscriberUseCase;
    private final ManageSubscriptionUseCase manageSubscriptionUseCase;

    @PostMapping
    public ResponseEntity<SubscriberSummary> register(
            @Valid @RequestBody RegisterSubscriberRequest request
    ) {
        SubscriberSummary summary = registerSubscriberUseCase.register(
                request.email(), request.preferredSendHour());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(summary.subscriberId()).toUri();
        return ResponseEntity.created(location).body(summary);
    }

    @PostMapping("/{subscriberId}/subscriptions")
    public ResponseEntity<SubscriptionSummary> subscribe(
            @PathVariable @Positive Long subscriberId,
            @Valid @RequestBody SubscribeRequest request
    ) {
        SubscriptionSummary summary = manageSubscriptionUseCase.subscribe(subscriberId, request.topicId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{topicId}")
                .buildAndExpand(request.topicId()).toUri();
        return ResponseEntity.created(location).body(summary);
    }

    @DeleteMapping("/{subscriberId}/subscriptions/{topicId}")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable @Positive Long subscriberId,
            @PathVariable @Positive Long topicId
    ) {
        manageSubscriptionUseCase.unsubscribe(subscriberId, topicId);
        return ResponseEntity.noContent().build();
    }
}
