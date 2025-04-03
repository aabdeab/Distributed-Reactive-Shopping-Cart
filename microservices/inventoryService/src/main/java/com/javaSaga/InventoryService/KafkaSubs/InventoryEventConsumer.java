// File: InventoryEventConsumer.java
package com.javaSaga.InventoryService.KafkaSubs;

import com.javaSaga.InventoryService.InventoryServiceImpl;
import com.javaSaga.events.Events.InventoryReservationEvent;
import com.javaSaga.events.Events.OrderCompletionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {

    private final InventoryServiceImpl inventoryService;

    @Autowired
    public InventoryEventConsumer(InventoryServiceImpl inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "inventory-reservation-topic")
    public void handleReservationRequest(InventoryReservationEvent event) {
        System.out.println("Received inventory reservation request for order: " + event.getOrderId());
        inventoryService.processReservation(event);
    }

    @KafkaListener(topics = "inventory-release-topic")
    public void handleReleaseRequest(InventoryReservationEvent event) {
        inventoryService.releaseReservation(event.getOrderId());
        System.out.println("Inventory released for order: " + event.getOrderId());
    }

    @KafkaListener(topics = "order-completion-topic")
    public void handleOrderCompletion(OrderCompletionEvent event) {
        inventoryService.handleOrderCompletion(event);
    }
}