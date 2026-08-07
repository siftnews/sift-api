package com.siftnews.subscriber;

import com.siftnews.content.api.TopicReference;
import com.siftnews.subscriber.application.port.in.ManageSubscriptionUseCase;
import com.siftnews.subscriber.application.port.in.RegisterSubscriberUseCase;
import com.siftnews.subscriber.application.port.in.SubscriptionSummary;
import com.siftnews.subscriber.application.port.out.SaveSubscriberPort;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.Subscriber;
import com.siftnews.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RegisterSubscriberUseCase registerSubscriberUseCase;

    @Autowired
    private ManageSubscriptionUseCase manageSubscriptionUseCase;

    @Autowired
    private SaveSubscriberPort saveSubscriberPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentDuplicateSubscriberRegistrationReturnsOneConflict() throws Exception {
        String email = "concurrent-" + UUID.randomUUID() + "@example.com";

        List<String> results = runConcurrently(() -> {
            try {
                registerSubscriberUseCase.register(email, 8);
                return "SAVED";
            } catch (ConflictException exception) {
                return "CONFLICT";
            }
        });

        assertThat(results).containsExactlyInAnyOrder("SAVED", "CONFLICT");
    }

    @Test
    void concurrentDuplicateSubscriptionReturnsOneConflict() throws Exception {
        Subscriber subscriber = saveSubscriberPort.save(
                Subscriber.create("subscription-" + UUID.randomUUID() + "@example.com", 8));
        Long topicId = createTopic();

        List<String> results = runConcurrently(() -> subscribe(subscriber.getSubscriberId(), topicId));

        assertThat(results).containsExactlyInAnyOrder("SAVED", "CONFLICT");
        assertThat(subscriptionCount(subscriber.getSubscriberId(), topicId)).isEqualTo(1);
    }

    @Test
    void concurrentPausedSubscriptionReactivationReturnsOneConflict() throws Exception {
        Subscriber subscriber = saveSubscriberPort.save(
                Subscriber.create("reactivation-" + UUID.randomUUID() + "@example.com", 8));
        Long topicId = createTopic();
        SubscriptionSummary initial = manageSubscriptionUseCase.subscribe(subscriber.getSubscriberId(), topicId);
        manageSubscriptionUseCase.unsubscribe(subscriber.getSubscriberId(), topicId);
        assertThat(subscriptionCount(subscriber.getSubscriberId(), topicId)).isEqualTo(1);

        List<String> results = runConcurrently(() -> subscribe(subscriber.getSubscriberId(), topicId));

        assertThat(results).containsExactlyInAnyOrder("SAVED", "CONFLICT");
        assertThat(subscriptionCount(subscriber.getSubscriberId(), topicId)).isEqualTo(1);
        assertThat(subscriptionStatus(subscriber.getSubscriberId(), topicId)).isEqualTo("ACTIVE");
        assertThat(initial.subscriptionId()).isPositive();
    }

    private String subscribe(Long subscriberId, Long topicId) {
        try {
            manageSubscriptionUseCase.subscribe(subscriberId, topicId);
            return "SAVED";
        } catch (ConflictException exception) {
            return "CONFLICT";
        }
    }

    private Long createTopic() {
        long topicId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        String slug = "concurrent-" + topicId;
        jdbcTemplate.update("""
                INSERT INTO topic (id, created_at, updated_at, name, slug, lang_scope,
                    include_keywords, exclude_keywords, keyword_weights, source_categories,
                    recency_half_life_hours, max_items, score_threshold, active)
                VALUES (?, now(), now(), ?, ?, 'ko,en', '[]'::jsonb, '[]'::jsonb,
                    '{}'::jsonb, '[\"DEV\"]'::jsonb, 24, 10, 0.0, true)
                """, topicId, "Concurrent topic " + topicId, slug);
        return topicId;
    }

    private int subscriptionCount(Long subscriberId, Long topicId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM subscription
                WHERE subscriber_id = ? AND topic_id = ?
                """, Integer.class, subscriberId, topicId);
    }

    private String subscriptionStatus(Long subscriberId, Long topicId) {
        return jdbcTemplate.queryForObject("""
                SELECT status
                FROM subscription
                WHERE subscriber_id = ? AND topic_id = ?
                """, String.class, subscriberId, topicId);
    }

    private List<String> runConcurrently(Callable<String> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Future<String>> futures = new ArrayList<>();
        try {
            Callable<String> synchronizedTask = () -> {
                barrier.await();
                return task.call();
            };
            futures.add(executor.submit(synchronizedTask));
            futures.add(executor.submit(synchronizedTask));
            return futures.stream().map(this::get).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private String get(Future<String> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
