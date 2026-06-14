package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "product-service", url = "http://product-service:8082")
public interface ProductClient {

    @GetMapping("/api/products/{id}/stock")
    int getStock(@PathVariable Long id);

    @PutMapping("/api/products/{id}/stock")
    void decreaseStock(@PathVariable Long id, @RequestParam int quantity);
}