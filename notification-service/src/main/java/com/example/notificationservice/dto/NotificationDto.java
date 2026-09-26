package com.example.notificationservice.dto;

import com.example.notificationservice.entity.NotificationHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

public class NotificationDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long orderId;
        private NotificationHistory.Type type;
        private String message;
        private LocalDateTime createdAt;

        public static Response from(NotificationHistory history) {
            return new Response(
                    history.getId(),
                    history.getOrderId(),
                    history.getType(),
                    history.getMessage(),
                    history.getCreatedAt()
            );
        }
    }
}
