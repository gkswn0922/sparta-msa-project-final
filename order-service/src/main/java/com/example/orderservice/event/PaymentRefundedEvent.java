package com.example.orderservice.event;

import com.example.orderservice.entity.Order;

import java.time.LocalDateTime;

public record PaymentRefundedEvent(
        Long orderId,
        Long userId,
        int amount,
        Order.PaymentMethod paymentMethod,
        LocalDateTime occurredAt
) {
}
