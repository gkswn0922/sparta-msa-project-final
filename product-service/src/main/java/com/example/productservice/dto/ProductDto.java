package com.example.productservice.dto;

import com.example.productservice.entity.Product;
import jakarta.validation.constraints.*;
import lombok.*;

public class ProductDto {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {

    @NotBlank
    private String name;

    @Min(0)
    private int price;

    @Min(0) @Max(100)
    private int discountRate;

    @NotBlank
    private String category;

    @Min(0)
    private int stock;

    private String description;
  }

  @Getter
  @Builder
  public static class Response {
    private Long id;
    private String name;
    private int price;
    private int discountRate;
    private int discountedPrice;
    private String category;
    private int stock;
    private String description;
    private Product.Status status;

    public static Response from(Product product) {
      return Response.builder()
          .id(product.getId())
          .name(product.getName())
          .price(product.getPrice())
          .discountRate(product.getDiscountRate())
          .discountedPrice(product.getDiscountedPrice())
          .category(product.getCategory())
          .stock(product.getStock())
          .description(product.getDescription())
          .status(product.getStatus())
          .build();
    }
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String name;
    private Integer price;
    private Integer discountRate;
    private String category;
    private Integer stock;
    private String description;
  }
}