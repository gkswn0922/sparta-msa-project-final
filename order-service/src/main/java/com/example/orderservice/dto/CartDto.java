package com.example.orderservice.dto;

import com.example.orderservice.entity.Cart;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

public class CartDto {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddRequest {

    @NotNull
    private Long productId;

    @NotBlank
    private String productName;

    @Min(0)
    private int price;

    @Min(1)
    private int quantity;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    @Min(1)
    private int quantity;
  }

  @Getter
  @Builder
  public static class ItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private int price;
    private int quantity;
    private int totalPrice;

    public static ItemResponse from(Cart cart) {
      return ItemResponse.builder()
          .id(cart.getId())
          .productId(cart.getProductId())
          .productName(cart.getProductName())
          .price(cart.getPrice())
          .quantity(cart.getQuantity())
          .totalPrice(cart.getPrice() * cart.getQuantity())
          .build();
    }
  }

  @Getter
  @Builder
  public static class CartResponse {
    private List<ItemResponse> items;
    private int totalAmount;
  }
}