package com.javaSaga.InventoryService;

import com.javaSaga.events.InventoryReservationEvent;
import com.javaSaga.events.OrderCompletionEvent;
import com.javaSaga.events.OrderItemDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {

    private final Map<Long, InventoryItem> inventory = new HashMap<>();
    private final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;


    // Initialize some sample inventory
    public InventoryService() {
        inventory.put(1L, new InventoryItem(1L, "Product 1", 100));
        inventory.put(2L, new InventoryItem(2L, "Product 2", 50));
        inventory.put(3L, new InventoryItem(3L, "Product 3", 75));
    }

    @KafkaListener(topics = "inventory-reservation-topic")
    public void handleReservationRequest(InventoryReservationEvent event) {
        System.out.println("Received inventory reservation request for order: " + event.getOrderId());

        boolean allItemsAvailable = true;
        StringBuilder failureReason = new StringBuilder();

        // Check inventory availability
        for (OrderItemDto item : event.getItems()) {
            InventoryItem inventoryItem = inventory.get(item.getProductId());
            if (inventoryItem == null || inventoryItem.getAvailableQuantity() < item.getQuantity()) {
                allItemsAvailable = false;
                failureReason.append("Insufficient inventory for product ")
                        .append(item.getProductName())
                        .append("with quantity"+item.getQuantity())
                        .append(". ");
            }
        }

        if (allItemsAvailable) {
            // Create reservations for all items
            Reservation reservation = new Reservation();
            reservation.setOrderId(event.getOrderId());
            reservation.setItems(event.getItems());
            reservation.setReservationTime(LocalDateTime.now());
            reservation.setExpiryTime(LocalDateTime.now().plusMinutes(15));

            // Update inventory
            for (OrderItemDto item : event.getItems()) {
                InventoryItem inventoryItem = inventory.get(item.getProductId());
                inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() + item.getQuantity());
            }

            reservations.put(event.getOrderId(), reservation);

            // Send success response
            InventoryReservationEvent responseEvent = InventoryReservationEvent.builder()
                    .orderId(event.getOrderId())
                    .items(event.getItems())
                    .status("SUCCESS")
                    .build();

            kafkaTemplate.send("inventory-response-topic", responseEvent);
            System.out.println("Inventory reserved for 15 minutes for order: " + event.getOrderId());
        } else {
            // Send failure response
            InventoryReservationEvent responseEvent = InventoryReservationEvent.builder()
                    .orderId(event.getOrderId())
                    .items(event.getItems())
                    .status("FAILED")
                    .reason(failureReason.toString())
                    .build();

            kafkaTemplate.send("inventory-response-topic", responseEvent);
            System.out.println("Inventory reservation failed: " + failureReason);
        }
    }

    @KafkaListener(topics = "inventory-release-topic")
    public void handleReleaseRequest(InventoryReservationEvent event) {
        releaseReservation(event.getOrderId());
        System.out.println("Inventory released for order: " + event.getOrderId());
    }

    @KafkaListener(topics = "order-completion-topic")
    public void handleOrderCompletion(OrderCompletionEvent event) {
        if ("COMPLETED".equals(event.getStatus())) {
            // Confirm reservation - convert reserved quantity to sold
            Reservation reservation = reservations.get(event.getOrderId());
            if (reservation != null) {
                for (OrderItemDto item : reservation.getItems()) {
                    InventoryItem inventoryItem = inventory.get(item.getProductId());
                    if (inventoryItem != null) {
                        inventoryItem.setAvailableQuantity(inventoryItem.getAvailableQuantity() - item.getQuantity());
                        inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() - item.getQuantity());
                    }
                }
                reservations.remove(event.getOrderId());
                System.out.println("Inventory confirmed for completed order: " + event.getOrderId());
            }
        } else if ("FAILED".equals(event.getStatus())) {
            // Release inventory for failed orders
            releaseReservation(event.getOrderId());
            System.out.println("Inventory released for failed order: " + event.getOrderId());
        }
    }

    private void releaseReservation(Long orderId) {
        Reservation reservation = reservations.get(orderId);
        if (reservation != null) {
            for (OrderItemDto item : reservation.getItems()) {
                InventoryItem inventoryItem = inventory.get(item.getProductId());
                if (inventoryItem != null) {
                    inventoryItem.setReservedQuantity(inventoryItem.getReservedQuantity() - item.getQuantity());
                }
            }
            reservations.remove(orderId);
        }
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        reservations.forEach((orderId, reservation) -> {
            if (now.isAfter(reservation.getExpiryTime())) {
                System.out.println("Reservation expired for order: " + orderId);
                releaseReservation(orderId);

                // Notify OrderService about expired reservation
                OrderCompletionEvent completionEvent = OrderCompletionEvent.builder()
                        .orderId(orderId)
                        .status("FAILED")
                        .reason("Reservation expired")
                        .build();

                kafkaTemplate.send("order-completion-topic", completionEvent);
            }
        });
    }
}
