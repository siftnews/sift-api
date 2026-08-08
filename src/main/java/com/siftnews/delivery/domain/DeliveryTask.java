package com.siftnews.delivery.domain;

import com.siftnews.common.BusinessException;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Getter
public class DeliveryTask {

    private final Long deliveryTaskId;
    private final Long deliveryJobId;
    private final Long issueId;
    private final Long subscriberId;
    private final String email;
    private final DeliveryTaskStatus status;
    private final int attemptCount;
    private final Instant nextRetryAt;
    private final String lastError;
    private final String idempotencyKey;

    private DeliveryTask(Long deliveryTaskId, Long deliveryJobId, Long issueId, Long subscriberId, String email,
                         DeliveryTaskStatus status, int attemptCount, Instant nextRetryAt, String lastError,
                         String idempotencyKey) {
        this.deliveryTaskId = deliveryTaskId;
        this.deliveryJobId = deliveryJobId;
        this.issueId = issueId;
        this.subscriberId = subscriberId;
        this.email = email;
        this.status = status;
        this.attemptCount = attemptCount;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
        this.idempotencyKey = idempotencyKey;
    }

    public static DeliveryTask pending(Long deliveryJobId, Long issueId, Long subscriberId, String email) {
        if (deliveryJobId == null || issueId == null || subscriberId == null || email == null || email.isBlank()) {
            throw new BusinessException("delivery task 생성 값은 비어 있을 수 없습니다.");
        }
        return new DeliveryTask(null, deliveryJobId, issueId, subscriberId, email, DeliveryTaskStatus.PENDING,
                0, null, null, key(issueId, subscriberId));
    }

    public static DeliveryTask restore(Long taskId, Long jobId, Long issueId, Long subscriberId, String email,
                                       DeliveryTaskStatus status, int attemptCount, Instant nextRetryAt,
                                       String lastError, String key) {
        return new DeliveryTask(taskId, jobId, issueId, subscriberId, email, status, attemptCount, nextRetryAt,
                lastError, key);
    }

    private static String key(Long issueId, Long subscriberId) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((issueId + ":" + subscriberId).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
