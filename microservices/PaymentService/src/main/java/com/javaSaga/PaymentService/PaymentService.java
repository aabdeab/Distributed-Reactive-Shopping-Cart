package com.javaSaga.PaymentService;

import com.javaSaga.events.Events.OrderCompletionEvent;
import com.javaSaga.events.Events.PaymentEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private final Map<Long, Payment> payments = new HashMap<>();
    private final Random random = new Random();

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "payment-request-topic")
    public void handlePaymentRequest(PaymentEvent event) {
        System.out.println("Payment request received for order: " + event.getOrderId());

        // Simulate payment processing
        boolean paymentSuccess = random.nextDouble() > 0.1; // 90% success rate

        if (paymentSuccess) {
            // Create payment record
            Payment payment = Payment.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .transactionId(UUID.randomUUID().toString())
                    .status("COMPLETED")
                    .timestamp(LocalDateTime.now())
                    .build();

            payments.put(event.getOrderId(), payment);

            // Send success response
            PaymentEvent responseEvent = PaymentEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .status("SUCCESS")
                    .transactionId(payment.getTransactionId())
                    .build();
            kafkaTemplate.send("payment-response-topic", responseEvent);
            System.out.println("Payment successful for order: " + event.getOrderId());
        } else {
            PaymentEvent responseEvent = PaymentEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .status("FAILED")
                    .reason("Payment processing error")
                    .build();

            kafkaTemplate.send("payment-response-topic", responseEvent);
            System.out.println("Payment failed for order: " + event.getOrderId());
        }
    }

    @KafkaListener(topics = "order-completion-topic")
    public void handleOrderCompletion(OrderCompletionEvent event) {
        if ("FAILED".equals(event.getStatus())) {
            // If order failed after payment was successful, process refund
            Payment payment = payments.get(event.getOrderId());
            if (payment != null && "COMPLETED".equals(payment.getStatus())) {
                payment.setStatus("REFUNDED");
                payment.setRefundReason(event.getReason());
                payment.setRefundTimestamp(LocalDateTime.now());
                System.out.println("Payment refunded for failed order: " + event.getOrderId());
            }
        }
    }
}