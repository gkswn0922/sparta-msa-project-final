package com.example.orderservice.controller;

import com.example.orderservice.dto.PaymentDto;
import com.example.orderservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제
    @PostMapping
    public ResponseEntity<PaymentDto.Response> pay(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PaymentDto.PayRequest request) {
        return ResponseEntity.ok(paymentService.pay(userId, request));
    }

    // 환불
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<PaymentDto.Response> refund(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.refund(userId, orderId));
    }

    @GetMapping("/success")
    public ResponseEntity<String> success() {
        return ResponseEntity.ok("success");
    }

    @GetMapping("/fail")
    public ResponseEntity<String> fail() {
        return ResponseEntity.ok("fail");
    }
}
