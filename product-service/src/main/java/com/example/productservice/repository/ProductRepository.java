package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findByStatusNot(Product.Status status);
  List<Product> findByCategoryAndStatusNot(String category, Product.Status status);
  List<Product> findByNameContainingIgnoreCaseAndStatusNot(String name, Product.Status status);
}