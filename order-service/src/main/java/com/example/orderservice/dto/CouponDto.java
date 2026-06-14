package com.example.orderservice.dto;

import com.example.orderservice.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class CouponDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank
        private String code;

        @NotBlank
        private String name;

        @NotNull
        private Coupon.DiscountType discountType;

        @Min(1)
        private int discountValue;

        @Min(0)
        private int minOrderAmount;

        private Long userId;

        @NotNull
        private LocalDateTime expiredAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UseRequest {
        @NotBlank
        private String code;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String code;
        private String name;
        private Coupon.DiscountType discountType;
        private int discountValue;
        private int minOrderAmount;
        private boolean isUsed;
        private LocalDateTime expiredAt;

        public static Response from(Coupon coupon) {
            return Response.builder()
                    .id(coupon.getId())
                    .code(coupon.getCode())
                    .name(coupon.getName())
                    .discountType(coupon.getDiscountType())
                    .discountValue(coupon.getDiscountValue())
                    .minOrderAmount(coupon.getMinOrderAmount())
                    .isUsed(coupon.isUsed())
                    .expiredAt(coupon.getExpiredAt())
                    .build();
        }
    }
}
