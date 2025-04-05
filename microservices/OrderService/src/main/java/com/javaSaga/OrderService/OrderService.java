package com.javaSaga.OrderService;

import com.javaSaga.events.Events.InventoryReservationEvent;
import com.javaSaga.events.Events.OrderCompletionEvent;
import com.javaSaga.events.Events.OrderCreationEvent;
import com.javaSaga.events.Events.PaymentEvent;
import com.javaSaga.events.DTOs.OrderItemDto;
import com.javaSaga.Exceptions.EmptyCartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.javaSaga.events.Models.Cart;
import com.javaSaga.events.Models.Product;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class OrderService {
    private final Map<Long, Order> orders = new HashMap<>();
    private long nextOrderId = 1L;
    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<Order> createOrder(OrderCreationEvent event) {
        Order order = Order.builder()
                .id(nextOrderId++)
                .userId(event.getUserId())
                .items(event.getItems())
                .status("PENDING")
                .totalAmount(event.getTotalAmount())
                .createdAt(LocalDateTime.now())
                .build();

        orders.put(order.getId(), order);
        InventoryReservationEvent reservationEvent = InventoryReservationEvent.builder()
                .orderId(order.getId())
                .items(order.getItems())
                .build();

        kafkaTemplate.send("inventory-reservation-topic", reservationEvent);

        System.out.println("Order created and inventory reservation requested: " + order.getId());
        return Mono.just(order);
    }

    public Mono<Void> checkout() {
        return webClientBuilder.build()
                .get()
                .uri("http://localhost:8080/api/cart")
                .retrieve()
                .bodyToMono(Cart.class)
                .flatMap(cart -> {
                    System.out.println("la carte contient " + cart.getProducts().size() + "produits");
                    if (cart.getProducts().size() == 0) {
                        return Mono.error(new EmptyCartException("checkout failed.Your Cart is Still Empty"));
                    } else {
                        double totalAmount = cart.getProducts().stream()
                                .mapToDouble(Product::getPrice)
                                .sum();
                        List<OrderItemDto> orderItems = cart.getProducts().stream()
                                .map(p -> OrderItemDto.builder()
                                        .productId(p.getId())
                                        .productName(p.getName())
                                        .price(p.getPrice())
                                        .Quantity(1) // Assuming quantity 1 for simplicity
                                        .build())
                                .collect(Collectors.toList());
                        OrderCreationEvent order = OrderCreationEvent.builder()
                                .userId(cart.getUserId())
                                .status("CREATED")
                                .items(orderItems)
                                .totalAmount(totalAmount)
                                .build();
                        return createOrder(order);
                    }
                })
                .doOnError(error -> System.err.println("Failed to create order: " + error.getMessage()))
                .then();

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
            order.setStatus("INVENTORY_RESERVED");
            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .amount(order.getTotalAmount())
                    .build();

            kafkaTemplate.send("payment-request-topic", paymentEvent);
            System.out.println("Inventory reserved, payment requested for order: " + order.getId());
        } else {
            order.setStatus("FAILED");
            order.setFailureReason(event.getReason());

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
            order.setStatus("COMPLETED");
            order.setTransactionId(event.getTransactionId());

            OrderCompletionEvent completionEvent = OrderCompletionEvent.builder()
                    .orderId(order.getId())
                    .status("COMPLETED")
                    .build();

            kafkaTemplate.send("order-completion-topic", completionEvent);
            System.out.println("Order completed successfully: " + order.getId());
        } else {
            order.setStatus("FAILED");
            order.setFailureReason("Payment failed: " + event.getReason());

            InventoryReservationEvent releaseEvent = InventoryReservationEvent.builder()
                    .orderId(order.getId())
                    .items(order.getItems())
                    .status("RELEASE")
                    .build();

            kafkaTemplate.send("inventory-release-topic", releaseEvent);

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