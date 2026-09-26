package com.example.orderservice.service;

import com.example.orderservice.dto.PaymentDto;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.event.PaymentCompletedEvent;
import com.example.orderservice.event.PaymentRefundedEvent;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    // 결제 승인
    @Transactional
    public PaymentDto.Response pay(Long userId, PaymentDto.PayRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 주문만 결제할 수 있습니다");
        }

        if (order.getStatus() != Order.Status.PENDING) {
            throw new IllegalArgumentException("결제대기 상태의 주문만 결제할 수 있습니다");
        }

        if (order.getTotalAmount() != request.getAmount()) {
            throw new IllegalArgumentException("결제 금액이 주문 금액과 다릅니다");
        }

        // 토스페이 승인 API 호출
        String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedKey);

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", request.getPaymentKey());
        body.put("orderId", request.getTossOrderId()); // 토스페이 orderId 그대로 사용
        body.put("amount", request.getAmount());

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    httpEntity,
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new IllegalArgumentException("결제 승인 실패");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 결제 저장
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .userId(userId)
                .amount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .build();

        payment.complete();
        paymentRepository.save(payment);
        order.updateStatus(Order.Status.PAID);

        eventPublisher.publishEvent(new PaymentCompletedEvent(
                order.getId(), userId, payment.getAmount(), payment.getPaymentMethod(), LocalDateTime.now()
        ));

        return PaymentDto.Response.from(payment);
    }

    // 환불
    @Transactional
    public PaymentDto.Response refund(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 주문만 환불할 수 있습니다");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역이 없습니다"));

        payment.refund();
        order.updateStatus(Order.Status.CANCELLED);

        eventPublisher.publishEvent(new PaymentRefundedEvent(
                order.getId(), userId, payment.getAmount(), payment.getPaymentMethod(), LocalDateTime.now()
        ));

        return PaymentDto.Response.from(payment);
    }
}