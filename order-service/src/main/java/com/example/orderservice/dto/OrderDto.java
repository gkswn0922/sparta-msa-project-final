package com.example.orderservice.dto;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderDto {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {

    @NotNull
    private Order.PaymentMethod paymentMethod;

    @NotBlank
    private String address;
    private String couponCode;

  }

  @Getter
  @Builder
  public static class ItemResponse {
    private Long productId;
    private String productName;
    private int price;
    private int quantity;
    private int totalPrice;

    public static ItemResponse from(OrderItem item) {
      return ItemResponse.builder()
          .productId(item.getProductId())
          .productName(item.getProductName())
          .price(item.getPrice())
          .quantity(item.getQuantity())
          .totalPrice(item.getTotalPrice())
          .build();
    }
  }

  @Getter
  @Builder
  public static class Response {
    private Long id;
    private int totalAmount;
    private Order.PaymentMethod paymentMethod;
    private Order.Status status;
    private String address;
    private List<ItemResponse> items;
    private LocalDateTime createdAt;

    public static Response from(Order order) {
      return Response.builder()
          .id(order.getId())
          .totalAmount(order.getTotalAmount())
          .paymentMethod(order.getPaymentMethod())
          .status(order.getStatus())
          .address(order.getAddress())
          .items(order.getItems().stream()
              .map(ItemResponse::from)
              .collect(Collectors.toList()))
          .createdAt(order.getCreatedAt())
          .build();
    }
  }
}