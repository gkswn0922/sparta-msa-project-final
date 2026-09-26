package com.example.orderservice.event;

import java.time.LocalDateTime;

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
