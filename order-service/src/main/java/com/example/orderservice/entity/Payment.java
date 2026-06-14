package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Order.PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = Status.PENDING;
    }

    public void complete() {
        this.status = Status.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }

    public void refund() {
        if (this.status != Status.COMPLETED) {
            throw new IllegalArgumentException("완료된 결제만 환불할 수 있습니다");
        }
        this.status = Status.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING,    // 대기
        COMPLETED,  // 완료
        FAILED,     // 실패
        REFUNDED    // 환불
    }
}
