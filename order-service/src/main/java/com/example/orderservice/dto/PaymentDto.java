package com.example.orderservice.dto;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class PaymentDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayRequest {
        @NotBlank
        private String paymentKey;

        @NotNull
        private Long orderId;

        @NotBlank
        private String tossOrderId; // ORDER-9-timestamp 형식

        @NotNull
        private int amount;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long orderId;
        private Long userId;
        private int amount;
        private Order.PaymentMethod paymentMethod;
        private Payment.Status status;
        private LocalDateTime paidAt;
        private LocalDateTime refundedAt;

        public static Response from(Payment payment) {
            return Response.builder()
                    .id(payment.getId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .paymentMethod(payment.getPaymentMethod())
                    .status(payment.getStatus())
                    .paidAt(payment.getPaidAt())
                    .refundedAt(payment.getRefundedAt())
                    .build();
        }
    }
}
