package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.entity.Order;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  // 주문 생성
  @PostMapping
  public ResponseEntity<OrderDto.Response> createOrder(
      @RequestHeader("X-User-Id") Long userId,
      @Valid @RequestBody OrderDto.CreateRequest request) {
    return ResponseEntity.ok(orderService.createOrder(userId, request));
  }

  // 주문 목록 조회
  @GetMapping
  public ResponseEntity<List<OrderDto.Response>> getOrders(
      @RequestHeader("X-User-Id") Long userId) {
    return ResponseEntity.ok(orderService.getOrders(userId));
  }

  // 주문 상세 조회
  @GetMapping("/{orderId}")
  public ResponseEntity<OrderDto.Response> getOrder(
      @RequestHeader("X-User-Id") Long userId,
      @PathVariable Long orderId) {
    return ResponseEntity.ok(orderService.getOrder(userId, orderId));
  }

  // 주문 취소
  @PatchMapping("/{orderId}/cancel")
  public ResponseEntity<OrderDto.Response> cancelOrder(
      @RequestHeader("X-User-Id") Long userId,
      @PathVariable Long orderId) {
    return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
  }

  // 주문 상태 변경 (관리자용)
  @PatchMapping("/{orderId}/status")
  public ResponseEntity<OrderDto.Response> updateOrderStatus(
          @PathVariable Long orderId,
          @RequestParam Order.Status status) {
    return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
  }
}