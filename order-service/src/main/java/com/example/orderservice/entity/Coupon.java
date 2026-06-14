package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private int discountValue; // 정률이면 %, 정액이면 원

    @Column(nullable = false)
    private int minOrderAmount; // 최소 주문금액

    private Long userId; // 특정 유저에게 발급된 경우

    @Column(nullable = false)
    private boolean isUsed;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isUsed = false;
    }

    public int calculateDiscount(int orderAmount) {
        if (orderAmount < minOrderAmount) {
            throw new IllegalArgumentException(
                "최소 주문금액 " + minOrderAmount + "원 이상이어야 합니다");
        }
        if (discountType == DiscountType.PERCENTAGE) {
            return orderAmount * discountValue / 100;
        } else {
            return discountValue;
        }
    }

    public void use() {
        if (isUsed) throw new IllegalArgumentException("이미 사용된 쿠폰입니다");
        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 쿠폰입니다");
        }
        this.isUsed = true;
    }

    public enum DiscountType {
        PERCENTAGE, // 정률 (%)
        FIXED       // 정액 (원)
    }
}
