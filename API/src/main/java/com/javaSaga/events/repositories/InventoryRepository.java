package com.javaSaga.events.repositories;

import com.javaSaga.events.Models.InventoryItem;
import com.javaSaga.events.Models.Reservation;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    Optional<InventoryItem> findItemById(Long productId);

    Optional<Reservation> findReservationById(Long productId);

    List<InventoryItem> findAllItems();
    void saveItem(InventoryItem item);
    void updateItemQuantity(Long productId, int availableQuantity, int reservedQuantity);

    void saveReservation(Reservation reservation);
    void deleteReservation(Long orderId);
    List<Reservation> findExpiredReservations();
}