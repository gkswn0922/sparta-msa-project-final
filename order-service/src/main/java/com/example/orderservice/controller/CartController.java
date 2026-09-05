package com.example.orderservice.controller;

import com.example.orderservice.dto.CartDto;
import com.example.orderservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

  private final CartService cartService;

  // 장바구니 담기
  @PostMapping
  public ResponseEntity<CartDto.ItemResponse> addToCart(
      @RequestHeader("X-User-Id") Long userId,
      @Valid @RequestBody CartDto.AddRequest request) {
    return ResponseEntity.ok(cartService.addToCart(userId, request));
  }

  // 장바구니 조회
  @GetMapping
  public ResponseEntity<CartDto.CartResponse> getCart(
      @RequestHeader("X-User-Id") Long userId) {
    return ResponseEntity.ok(cartService.getCart(userId));
  }

  // 수량 변경
  @PatchMapping("/{cartId}")
  public ResponseEntity<CartDto.ItemResponse> updateQuantity(
      @RequestHeader("X-User-Id") Long userId,
      @PathVariable Long cartId,
      @Valid @RequestBody CartDto.UpdateRequest request) {
    return ResponseEntity.ok(cartService.updateQuantity(userId, cartId, request));
  }

  // 장바구니 항목 삭제
  @DeleteMapping("/{cartId}")
  public ResponseEntity<Void> removeFromCart(
      @RequestHeader("X-User-Id") Long userId,
      @PathVariable Long cartId) {
    cartService.removeFromCart(userId, cartId);
    return ResponseEntity.noContent().build();
  }
}