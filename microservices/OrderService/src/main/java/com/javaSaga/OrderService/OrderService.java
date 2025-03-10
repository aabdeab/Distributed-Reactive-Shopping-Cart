package com.javaSaga.OrderService;

import com.javaSaga.events.InventoryReservationEvent;
import com.javaSaga.events.OrderCompletionEvent;
import com.javaSaga.events.OrderCreationEvent;
import com.javaSaga.events.PaymentEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
class OrderService {

    private final Map<Long, Order> orders = new HashMap<>();
    private long nextOrderId = 1L;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public Order createOrder(OrderCreationEvent event) {
        // Create a new order with PENDING status
        Order order = Order.builder()
                .id(nextOrderId++)
                .userId(event.getUserId())
                .items(event.getItems())
                .status("PENDING")
                .totalAmount(event.getTotalAmount())
                .createdAt(LocalDateTime.now())
                .build();

        orders.put(order.getId(), order);

        // Publish inventory reservation request to Kafka
        InventoryReservationEvent reservationEvent = InventoryReservationEvent.builder()
                .orderId(order.getId())
                .items(order.getItems())
                .build();

        kafkaTemplate.send("inventory-reservation-topic", reservationEvent);

        System.out.println("Order created and inventory reservation requested: " + order.getId());
        return order;
    }

    public Order getOrder(Long orderId) {
        return orders.getOrDefault(orderId, null);
    }

    @KafkaListener(topics = "inventory-response-topic")
    public void handleInventoryResponse(InventoryReservationEvent event) {
        Order order = orders.get(event.getOrderId());
        if (order == null) {
            System.err.println("Order not found: " + event.getOrderId());
            return;
        }

        if ("SUCCESS".equals(event.getStatus())) {
            // Update order status
            order.setStatus("INVENTORY_RESERVED");

            // Request payment
            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .amount(order.getTotalAmount())
                    .build();

            kafkaTemplate.send("payment-request-topic", paymentEvent);
            System.out.println("Inventory reserved, payment requested for order: " + order.getId());
        } else {
            // Inventory reservation failed
            order.setStatus("FAILED");
            order.setFailureReason(event.getReason());

            // Notify about order failure
            OrderCompletionEvent completionEvent = OrderCompletionEvent.builder()
                    .orderId(order.getId())
                    .status("FAILED")
                    .reason("Inventory reservation failed: " + event.getReason())
                    .build();

            kafkaTemplate.send("order-completion-topic", completionEvent);
            System.out.println("Order failed due to inventory issues: " + order.getId());
        }
    }

    @KafkaListener(topics = "payment-response-topic")
    public void handlePaymentResponse(PaymentEvent event) {
        Order order = orders.get(event.getOrderId());
        if (order == null) {
            System.err.println("Order not found: " + event.getOrderId());
            return;
        }

        if ("SUCCESS".equals(event.getStatus())) {
            // Update order status
            order.setStatus("COMPLETED");
            order.setTransactionId(event.getTransactionId());

            // Notify all services about successful completion
            OrderCompletionEvent completionEvent = OrderCompletionEvent.builder()
                    .orderId(order.getId())
                    .status("COMPLETED")
                    .build();

            kafkaTemplate.send("order-completion-topic", completionEvent);
            System.out.println("Order completed successfully: " + order.getId());
        } else {
            // Payment failed
            order.setStatus("FAILED");
            order.setFailureReason("Payment failed: " + event.getReason());

            // Release inventory (compensating transaction)
            InventoryReservationEvent releaseEvent = InventoryReservationEvent.builder()
                    .orderId(order.getId())
                    .items(order.getItems())
                    .status("RELEASE")
                    .build();

            kafkaTemplate.send("inventory-release-topic", releaseEvent);

            // Notify about order failure
            OrderCompletionEvent completionEvent = OrderCompletionEvent.builder()
                    .orderId(order.getId())
                    .status("FAILED")
                    .reason("Payment failed: " + event.getReason())
                    .build();

            kafkaTemplate.send("order-completion-topic", completionEvent);
            System.out.println("Order failed due to payment issues: " + order.getId());
        }
    }
}
