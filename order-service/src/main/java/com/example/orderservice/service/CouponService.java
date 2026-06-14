package com.example.orderservice.service;

import com.example.orderservice.dto.CouponDto;
import com.example.orderservice.entity.Coupon;
import com.example.orderservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    // 쿠폰 발급 (관리자)
    public CouponDto.Response createCoupon(CouponDto.CreateRequest request) {
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 쿠폰 코드입니다");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .userId(request.getUserId())
                .expiredAt(request.getExpiredAt())
                .build();

        return CouponDto.Response.from(couponRepository.save(coupon));
    }

    // 내 쿠폰 목록 조회
    public List<CouponDto.Response> getMyCoupons(Long userId) {
        return couponRepository.findByUserIdAndIsUsed(userId, false)
                .stream()
                .map(CouponDto.Response::from)
                .collect(Collectors.toList());
    }

    // 쿠폰 할인 금액 계산
    @Transactional
    public int applyCoupon(String code, int orderAmount) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다"));

        int discountAmount = coupon.calculateDiscount(orderAmount);
        coupon.use();
        return discountAmount;
    }
}
