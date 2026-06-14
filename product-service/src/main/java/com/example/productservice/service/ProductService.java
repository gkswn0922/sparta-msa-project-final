package com.example.productservice.service;

import com.example.productservice.dto.ProductDto;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;

  // 상품 등록
  public ProductDto.Response createProduct(ProductDto.CreateRequest request) {
    Product product = Product.builder()
        .name(request.getName())
        .price(request.getPrice())
        .discountRate(request.getDiscountRate())
        .category(request.getCategory())
        .stock(request.getStock())
        .description(request.getDescription())
        .build();

    return ProductDto.Response.from(productRepository.save(product));
  }

  // 상품 목록 조회 (삭제된 상품 제외)
  public List<ProductDto.Response> getProducts(String category) {
    List<Product> products;

    if (category != null && !category.isEmpty()) {
      products = productRepository.findByCategoryAndStatusNot(category, Product.Status.DELETED);
    } else {
      products = productRepository.findByStatusNot(Product.Status.DELETED);
    }

    return products.stream()
        .map(ProductDto.Response::from)
        .collect(Collectors.toList());
  }

  // 상품 상세 조회
  public ProductDto.Response getProduct(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다"));

    if (product.getStatus() == Product.Status.DELETED) {
      throw new IllegalArgumentException("삭제된 상품입니다");
    }

    return ProductDto.Response.from(product);
  }

  // 상품 수정
  @Transactional
  public ProductDto.Response updateProduct(Long id, ProductDto.UpdateRequest request) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다"));

    if (product.getStatus() == Product.Status.DELETED) {
      throw new IllegalArgumentException("삭제된 상품입니다");
    }

    product.update(request.getName(), request.getPrice(), request.getDiscountRate(),
            request.getCategory(), request.getStock(), request.getDescription());

    return ProductDto.Response.from(product);
  }

  // 상품 삭제 (soft delete)
  @Transactional
  public void deleteProduct(Long id) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다"));

    product.delete();
  }

  // 재고 조회
  public int getStock(Long id) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다"));
    return product.getStock();
  }

  // 재고 차감
  @Transactional
  public void decreaseStock(Long id, int quantity) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다"));

    if (product.getStock() < quantity) {
      throw new IllegalArgumentException("재고가 부족합니다");
    }

    product.decreaseStock(quantity);
  }

  // 상품 검색
  public List<ProductDto.Response> searchProducts(String keyword) {
    return productRepository.findByNameContainingIgnoreCaseAndStatusNot(
                    keyword, Product.Status.DELETED)
            .stream()
            .map(ProductDto.Response::from)
            .collect(Collectors.toList());
  }
}