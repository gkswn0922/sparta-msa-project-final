package com.example.notificationservice.repository;

import com.example.notificationservice.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    List<NotificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
