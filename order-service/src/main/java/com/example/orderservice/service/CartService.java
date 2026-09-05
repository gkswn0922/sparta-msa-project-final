package com.example.orderservice.service;

import com.example.orderservice.dto.CartDto;
import com.example.orderservice.entity.Cart;
import com.example.orderservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

  private final CartRepository cartRepository;

  // 장바구니 담기
  @Transactional
  public CartDto.ItemResponse addToCart(Long userId, CartDto.AddRequest request) {
    // 이미 담긴 상품이면 수량 추가
    Cart cart = cartRepository.findByUserIdAndProductId(userId, request.getProductId())
        .map(existing -> {
          existing.updateQuantity(existing.getQuantity() + request.getQuantity());
          return existing;
        })
        .orElseGet(() -> Cart.builder()
            .userId(userId)
            .productId(request.getProductId())
            .productName(request.getProductName())
            .price(request.getPrice())
            .quantity(request.getQuantity())
            .build());

    return CartDto.ItemResponse.from(cartRepository.save(cart));
  }

  // 장바구니 조회
  public CartDto.CartResponse getCart(Long userId) {
    List<CartDto.ItemResponse> items = cartRepository.findByUserId(userId)
        .stream()
        .map(CartDto.ItemResponse::from)
        .collect(Collectors.toList());

    int totalAmount = items.stream()
        .mapToInt(CartDto.ItemResponse::getTotalPrice)
        .sum();

    return CartDto.CartResponse.builder()
        .items(items)
        .totalAmount(totalAmount)
        .build();
  }

  // 수량 변경
  @Transactional
  public CartDto.ItemResponse updateQuantity(Long userId, Long cartId, CartDto.UpdateRequest request) {
    Cart cart = cartRepository.findById(cartId)
        .orElseThrow(() -> new IllegalArgumentException("장바구니 항목이 없습니다"));

    if (!cart.getUserId().equals(userId)) {
      throw new IllegalArgumentException("본인의 장바구니만 수정할 수 있습니다");
    }

    cart.updateQuantity(request.getQuantity());
    return CartDto.ItemResponse.from(cartRepository.save(cart));
  }

  // 장바구니 항목 삭제
  @Transactional
  public void removeFromCart(Long userId, Long cartId) {
    Cart cart = cartRepository.findById(cartId)
        .orElseThrow(() -> new IllegalArgumentException("장바구니 항목이 없습니다"));

    if (!cart.getUserId().equals(userId)) {
      throw new IllegalArgumentException("본인의 장바구니만 삭제할 수 있습니다");
    }

    cartRepository.delete(cart);
  }
}