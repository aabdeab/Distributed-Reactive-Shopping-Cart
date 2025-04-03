// File: InventoryEventProducer.java
package com.javaSaga.InventoryService.KafkaSubs;

import com.javaSaga.events.Events.InventoryReservationEvent;
import com.javaSaga.events.Events.OrderCompletionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public InventoryEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendReservationResponse(InventoryReservationEvent event) {
        kafkaTemplate.send("inventory-response-topic", event);
    }

    public void sendReservationExpiration(OrderCompletionEvent event) {
        kafkaTemplate.send("order-completion-topic", event);
    }
}