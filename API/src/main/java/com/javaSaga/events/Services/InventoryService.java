package com.javaSaga.events.Services;

import com.javaSaga.events.DTOs.OrderItemDto;
import com.javaSaga.events.Events.InventoryReservationEvent;
import com.javaSaga.events.Events.OrderCompletionEvent;

public interface InventoryService {

    void processReservation(InventoryReservationEvent event);

    void releaseReservation(Long orderId);

    void handleOrderCompletion(OrderCompletionEvent event);

    void checkExpiredReservations();
}
