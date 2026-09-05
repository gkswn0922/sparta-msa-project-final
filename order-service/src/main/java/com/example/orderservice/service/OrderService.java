package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.dto.CartDto;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.entity.Cart;
import com.example.orderservice.entity.Coupon;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.repository.CouponRepository;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final CartRepository cartRepository;
  private final ProductClient productClient;
  private final CouponRepository couponRepository;
  private final StockLockService stockLockService;


  // 주문 생성 (장바구니 → 주문)
  @Transactional
  public OrderDto.Response createOrder(Long userId, OrderDto.CreateRequest request) {
    // 장바구니 조회
    List<Cart> cartItems = cartRepository.findByUserId(userId);
    if (cartItems.isEmpty()) {
      throw new IllegalArgumentException("장바구니가 비어있습니다");
    }

    // 총액 계산
    int totalAmount = cartItems.stream()
        .mapToInt(cart -> cart.getPrice() * cart.getQuantity())
        .sum();

    // 쿠폰 검증 및 할인 계산
    Coupon coupon = null;
    int discountAmount = 0;

    if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
      coupon = couponRepository.findByCode(request.getCouponCode())
          .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다"));
      coupon.validate(userId, totalAmount);
      discountAmount = coupon.calculateDiscount(totalAmount);
    }

    int finalAmount = Math.max(0, totalAmount - discountAmount);


    // 재고 차감 전에 먼저 검증
    for (Cart cart : cartItems) {
      int stock = productClient.getStock(cart.getProductId());
      if (stock < cart.getQuantity()) {
        throw new IllegalArgumentException(
            "[" + cart.getProductName() + "] 재고 부족. " +
                "현재: " + stock + "개, 요청: " + cart.getQuantity() + "개"
        );
      }
    }

    // 재고 차감
    cartItems.forEach(cart ->
        stockLockService.decreaseStockWithLock(cart.getProductId(), cart.getQuantity())
    );

    // 주문 생성
    Order order = Order.builder()
        .userId(userId)
        .totalAmount(totalAmount)
        .paymentMethod(request.getPaymentMethod())
        .address(request.getAddress())
        .build();

    // 주문 상품 생성
    List<OrderItem> orderItems = cartItems.stream()
        .map(cart -> OrderItem.builder()
            .order(order)
            .productId(cart.getProductId())
            .productName(cart.getProductName())
            .price(cart.getPrice())
            .quantity(cart.getQuantity())
            .build())
        .collect(Collectors.toList());

    order.getItems().addAll(orderItems);
    orderRepository.save(order);

    // 장바구니 비우기
    cartRepository.deleteByUserId(userId);

    return OrderDto.Response.from(order);
  }

  // 주문 목록 조회
  public List<OrderDto.Response> getOrders(Long userId) {
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
        .stream()
        .map(OrderDto.Response::from)
        .collect(Collectors.toList());
  }

  // 주문 상세 조회
  public OrderDto.Response getOrder(Long userId, Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다"));

    if (!order.getUserId().equals(userId)) {
      throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다");
    }

    return OrderDto.Response.from(order);
  }

  // 주문 취소
  @Transactional
  public OrderDto.Response cancelOrder(Long userId, Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다"));

    if (!order.getUserId().equals(userId)) {
      throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다");
    }

    if (order.getStatus() != Order.Status.PENDING) {
      throw new IllegalArgumentException("결제대기 상태의 주문만 취소할 수 있습니다");
    }

    order.updateStatus(Order.Status.CANCELLED);
    return OrderDto.Response.from(order);
  }

  // 주문 상태 변경 (관리자용)
  @Transactional
  public OrderDto.Response updateOrderStatus(Long orderId, Order.Status status) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다"));

    order.updateStatus(status);
    return OrderDto.Response.from(order);
  }
}