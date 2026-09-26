package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationDto;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 내 알림 이력 조회
    @GetMapping
    public ResponseEntity<List<NotificationDto.Response>> getNotifications(
            @RequestHeader("X-User-Id") Long userId) {
        List<NotificationDto.Response> response = notificationService.getNotifications(userId)
                .stream()
                .map(NotificationDto.Response::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
