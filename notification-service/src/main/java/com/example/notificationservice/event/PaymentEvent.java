package com.example.notificationservice.event;

import java.time.LocalDateTime;

/**
 * order-service가 payment-events 토픽에 발행하는 이벤트의 컨슈머 측 계약.
 * 별도 공유 모듈 없이 서비스별로 동일한 필드 구조를 각자 유지한다.
 */
public record PaymentEvent(
        String eventId,
        String eventType,
        Long orderId,
        Long userId,
        int amount,
        String paymentMethod,
        LocalDateTime occurredAt
) {
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
}
