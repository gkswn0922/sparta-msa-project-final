package com.example.orderservice.controller;

import com.example.orderservice.dto.CouponDto;
import com.example.orderservice.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // 쿠폰 발급 (관리자)
    @PostMapping
    public ResponseEntity<CouponDto.Response> createCoupon(
            @Valid @RequestBody CouponDto.CreateRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    // 내 쿠폰 목록 조회
    @GetMapping("/my")
    public ResponseEntity<List<CouponDto.Response>> getMyCoupons(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(couponService.getMyCoupons(userId));
    }

    // 쿠폰 적용 (할인 금액 계산)
    @PostMapping("/apply")
    public ResponseEntity<Integer> applyCoupon(
            @RequestParam String code,
            @RequestParam int orderAmount) {
        return ResponseEntity.ok(couponService.applyCoupon(code, orderAmount));
    }
}
