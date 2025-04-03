package com.javaSaga.InventoryService;

import com.javaSaga.events.Models.InventoryItem;
import com.javaSaga.events.Models.Reservation;
import com.javaSaga.events.repositories.InventoryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryInventoryRepository implements InventoryRepository {
    private final Map<Long, InventoryItem> inventory = new HashMap<>();
    private final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();
    public InMemoryInventoryRepository() {
        inventory.put(1L, new InventoryItem(1L, "Product 1", 100));
        inventory.put(2L, new InventoryItem(2L, "Product 2", 50));
        inventory.put(3L, new InventoryItem(3L, "Product 3", 75));
    }

    @Override
    public Optional<InventoryItem> findItemById(Long productId) {
        return Optional.ofNullable(inventory.get(productId));
    }

    @Override
    public List<InventoryItem> findAllItems() {
        return new ArrayList<>(inventory.values());
    }

    @Override
    public void saveItem(InventoryItem item) {
        inventory.put(item.getProductId(), item);
    }

    @Override
    public void updateItemQuantity(Long productId, int availableQuantity, int reservedQuantity) {
        InventoryItem item = inventory.get(productId);
        if (item != null) {
            item.setAvailableQuantity(availableQuantity);
            item.setReservedQuantity(reservedQuantity);
        }
    }

    @Override
    public void saveReservation(Reservation reservation) {
        reservations.put(reservation.getOrderId(), reservation);
    }

    @Override
    public Optional<Reservation> findReservationByOrderId(Long orderId) {
        return Optional.ofNullable(reservations.get(orderId));
    }

    @Override
    public void deleteReservation(Long orderId) {
        reservations.remove(orderId);
    }

    @Override
    public List<Reservation> findExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        return reservations.values().stream()
                .filter(reservation -> now.isAfter(reservation.getExpiryTime()))
                .collect(Collectors.toList());
    }
}