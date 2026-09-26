package com.example.orderservice.service;

import com.example.orderservice.dto.PaymentDto;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.event.PaymentCompletedEvent;
import com.example.orderservice.event.PaymentRefundedEvent;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 10L;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    RestTemplate restTemplate;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "tossSecretKey", "test_sk_dummy");
    }

    private Order order(Order.Status status) {
        return Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .totalAmount(30000)
                .paymentMethod(Order.PaymentMethod.CARD)
                .status(status)
                .address("서울")
                .build();
    }

    private PaymentDto.PayRequest payRequest(int amount) {
        return new PaymentDto.PayRequest("pk_123", ORDER_ID, "ORDER-10-1700000000", amount);
    }

    private ResponseEntity<Map> tossOk() {
        return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
    }

    @Nested
    @DisplayName("결제 승인")
    class Pay {

        @Test
        @DisplayName("성공하면 결제가 완료되고 주문은 PAID, 결제완료 이벤트가 발행된다")
        void success() {
            Order order = order(Order.Status.PENDING);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).willReturn(tossOk());

            PaymentDto.Response response = paymentService.pay(USER_ID, payRequest(30000));

            assertThat(response.getStatus()).isEqualTo(Payment.Status.COMPLETED);
            assertThat(response.getAmount()).isEqualTo(30000);
            assertThat(response.getPaidAt()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(Order.Status.PAID);
            verify(paymentRepository).save(any(Payment.class));

            ArgumentCaptor<PaymentCompletedEvent> event = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
            verify(eventPublisher).publishEvent(event.capture());
            assertThat(event.getValue().orderId()).isEqualTo(ORDER_ID);
            assertThat(event.getValue().amount()).isEqualTo(30000);
        }

        @Test
        @DisplayName("토스 승인 API에 시크릿 키를 Basic 인증으로 담아 호출한다")
        void callsTossWithBasicAuth() {
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(Order.Status.PENDING)));
            given(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).willReturn(tossOk());

            paymentService.pay(USER_ID, payRequest(30000));

            ArgumentCaptor<HttpEntity> request = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(eq(TOSS_CONFIRM_URL), request.capture(), eq(Map.class));
            String expected = "Basic " + Base64.getEncoder().encodeToString("test_sk_dummy:".getBytes());
            assertThat(request.getValue().getHeaders().getFirst("Authorization")).isEqualTo(expected);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) request.getValue().getBody();
            assertThat(body)
                    .containsEntry("paymentKey", "pk_123")
                    .containsEntry("orderId", "ORDER-10-1700000000")
                    .containsEntry("amount", 30000);
        }

        @Test
        @DisplayName("다른 사용자의 주문은 결제할 수 없고 토스 API도 호출하지 않는다")
        void notOwner() {
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(Order.Status.PENDING)));

            assertThatThrownBy(() -> paymentService.pay(999L, payRequest(30000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("본인의 주문만 결제할 수 있습니다");
            verifyNoInteractions(restTemplate, paymentRepository, eventPublisher);
        }

        @Test
        @DisplayName("결제대기 상태가 아닌 주문은 결제할 수 없다 (중복 결제 방지)")
        void notPending() {
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(Order.Status.PAID)));

            assertThatThrownBy(() -> paymentService.pay(USER_ID, payRequest(30000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결제대기 상태의 주문만 결제할 수 있습니다");
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("요청 금액이 주문 금액과 다르면 결제할 수 없다 (금액 위변조 방지)")
        void amountMismatch() {
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(Order.Status.PENDING)));

            assertThatThrownBy(() -> paymentService.pay(USER_ID, payRequest(100)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결제 금액이 주문 금액과 다릅니다");
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("토스 API 호출이 실패하면 결제를 저장하지 않고 주문 상태도 바뀌지 않는다")
        void tossFailure() {
            Order order = order(Order.Status.PENDING);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .willThrow(new RestClientException("timeout"));

            assertThatThrownBy(() -> paymentService.pay(USER_ID, payRequest(30000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("결제 처리 중 오류가 발생했습니다");
            assertThat(order.getStatus()).isEqualTo(Order.Status.PENDING);
            verify(paymentRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void orderNotFound() {
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.pay(USER_ID, payRequest(30000)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다");
        }
    }

    @Nested
    @DisplayName("환불")
    class Refund {

        private Payment payment(Payment.Status status) {
            return Payment.builder()
                    .id(100L)
                    .orderId(ORDER_ID)
                    .userId(USER_ID)
                    .amount(30000)
                    .paymentMethod(Order.PaymentMethod.CARD)
                    .status(status)
                    .build();
        }

        @Test
        @DisplayName("완료된 결제를 환불하면 REFUNDED, 주문은 CANCELLED, 환불 이벤트가 발행된다")
        void success() {
            Order order = order(Order.Status.PAID);
            Payment payment = payment(Payment.Status.COMPLETED);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));

            PaymentDto.Response response = paymentService.refund(USER_ID, ORDER_ID);

            assertThat(response.getStatus()).isEqualTo(Payment.Status.REFUNDED);
            assertThat(response.getRefundedAt()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(Order.Status.CANCELLED);

            ArgumentCaptor<PaymentRefundedEvent> event = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
            verify(eventPublisher).publishEvent(event.capture());
            assertThat(event.getValue().orderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("이미 환불된 결제는 다시 환불할 수 없고 주문 상태도 바뀌지 않는다")
        void alreadyRefunded() {
            Order order = order(Order.Status.CANCELLED);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment(Payment.Status.REFUNDED)));

            assertThatThrownBy(() -> paymentService.refund(USER_ID, ORDER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("완료된 결제만 환불할 수 있습니다");
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("다른 사용자의 주문은 환불할 수 없다")
        void notOwner() {
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(Order.Status.PAID)));

            assertThatThrownBy(() -> paymentService.refund(999L, ORDER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("본인의 주문만 환불할 수 있습니다");
            verifyNoInteractions(paymentRepository, eventPublisher);
        }

        @Test
        @DisplayName("결제 내역이 없으면 환불할 수 없다")
        void noPayment() {
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order(Order.Status.PENDING)));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.refund(USER_ID, ORDER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결제 내역이 없습니다");
        }
    }
}
