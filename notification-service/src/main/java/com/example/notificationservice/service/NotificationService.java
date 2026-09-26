package com.example.notificationservice.service;

import com.example.notificationservice.entity.NotificationHistory;
import com.example.notificationservice.event.PaymentEvent;
import com.example.notificationservice.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        NotificationHistory.Type type = NotificationHistory.Type.valueOf(event.eventType());
        String message = buildMessage(type, event);

        NotificationHistory history = NotificationHistory.builder()
                .orderId(event.orderId())
                .userId(event.userId())
                .type(type)
                .message(message)
                .build();

        notificationHistoryRepository.save(history);
        log.info("알림 발송 시뮬레이션 - userId={}, message={}", event.userId(), message);
    }

    private String buildMessage(NotificationHistory.Type type, PaymentEvent event) {
        return switch (type) {
            case PAYMENT_COMPLETED -> "주문 #" + event.orderId() + " 결제가 완료되었습니다. (" + event.amount() + "원)";
            case PAYMENT_REFUNDED -> "주문 #" + event.orderId() + " 결제가 환불되었습니다. (" + event.amount() + "원)";
        };
    }

    public List<NotificationHistory> getNotifications(Long userId) {
        return notificationHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
