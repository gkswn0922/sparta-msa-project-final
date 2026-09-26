package com.example.orderservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 트랜잭션이 커밋된 뒤에만 Kafka로 이벤트를 발행한다.
 * 커밋 전에 발행하면 이후 롤백 시에도 이벤트가 이미 나가버리는 dual-write 문제가 생기기 때문.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        publish(PaymentEvent.PAYMENT_COMPLETED, event.orderId(), event.userId(),
                event.amount(), event.paymentMethod().name(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        publish(PaymentEvent.PAYMENT_REFUNDED, event.orderId(), event.userId(),
                event.amount(), event.paymentMethod().name(), event.occurredAt());
    }

    private void publish(String eventType, Long orderId, Long userId, int amount,
                          String paymentMethod, LocalDateTime occurredAt) {
        PaymentEvent payload = new PaymentEvent(
                UUID.randomUUID().toString(), eventType, orderId, userId, amount, paymentMethod, occurredAt
        );
        kafkaTemplate.send(TOPIC, String.valueOf(orderId), payload);
        log.info("Kafka 이벤트 발행: type={}, orderId={}", eventType, orderId);
    }
}
