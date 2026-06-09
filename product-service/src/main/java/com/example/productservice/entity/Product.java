package com.example.productservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private int price;

  @Column(nullable = false)
  private int discountRate; // 0~100 (%)

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private int stock;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    if (this.status == null) this.status = Status.ON_SALE;
    if (this.discountRate == 0) this.discountRate = 0;
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public int getDiscountedPrice() {
    return price - (price * discountRate / 100);
  }

  public enum Status {
    ON_SALE,   // 판매중
    SOLD_OUT,  // 품절
    DELETED    // 삭제
  }
}