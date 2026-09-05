package com.example.productservice.controller;

import com.example.productservice.dto.ProductDto;
import com.example.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  // 상품 등록
  @PostMapping
  public ResponseEntity<ProductDto.Response> createProduct(
      @Valid @RequestBody ProductDto.CreateRequest request) {
    return ResponseEntity.ok(productService.createProduct(request));
  }

  // 상품 목록 조회
  @GetMapping
  public ResponseEntity<List<ProductDto.Response>> getProducts(
      @RequestParam(required = false) String category) {
    return ResponseEntity.ok(productService.getProducts(category));
  }

  // 상품 상세 조회
  @GetMapping("/{id}")
  public ResponseEntity<ProductDto.Response> getProduct(@PathVariable Long id) {
    return ResponseEntity.ok(productService.getProduct(id));
  }

  // 상품 수정
  @PutMapping("/{id}")
  public ResponseEntity<ProductDto.Response> updateProduct(
          @PathVariable Long id,
          @RequestBody ProductDto.UpdateRequest request) {
    return ResponseEntity.ok(productService.updateProduct(id, request));
  }

  // 상품 삭제
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  // 재고 조회
  @GetMapping("/{id}/stock")
  public ResponseEntity<Integer> getStock(@PathVariable Long id) {
    return ResponseEntity.ok(productService.getStock(id));
  }

  // 재고 차감
  @PutMapping("/{id}/stock")
  public ResponseEntity<Void> decreaseStock(
          @PathVariable Long id,
          @RequestParam int quantity) {
    productService.decreaseStock(id, quantity);
    return ResponseEntity.ok().build();
  }

  // 상품 검색
  @GetMapping("/search")
  public ResponseEntity<List<ProductDto.Response>> searchProducts(
          @RequestParam String keyword) {
    return ResponseEntity.ok(productService.searchProducts(keyword));
  }
}