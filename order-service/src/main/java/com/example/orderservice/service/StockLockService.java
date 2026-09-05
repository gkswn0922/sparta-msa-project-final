package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockLockService {

  private final RedissonClient redissonClient;
  private final ProductClient productClient;

  private static final String LOCK_KEY = "lock:product:";

  public void decreaseStockWithLock(Long productId, int quantity) {
    RLock lock = redissonClient.getLock(LOCK_KEY + productId);

    try {
      // 최대 3초 대기, 락 획득 후 5초 안에 해제
      boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);

      if (!acquired) {
        throw new IllegalStateException("잠시 후 다시 시도해주세요");
      }

      productClient.decreaseStock(productId, quantity);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("재고 처리 중 오류가 발생했습니다");
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }
}