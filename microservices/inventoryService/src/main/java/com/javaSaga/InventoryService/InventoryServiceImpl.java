package com.javaSaga.InventoryService;

import com.javaSaga.InventoryService.kafka.InventoryEventProducer;
import com.javaSaga.events.DTOs.OrderItemDto;
import com.javaSaga.events.Events.InventoryReservationEvent;
import com.javaSaga.events.Events.OrderCompletionEvent;
import com.javaSaga.events.Models.InventoryItem;
import com.javaSaga.events.Models.Reservation;
import com.javaSaga.events.Services.InventoryService;
import com.javaSaga.events.repositories.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer eventProducer;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository, InventoryEventProducer eventProducer) {
        this.inventoryRepository = inventoryRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public void processReservation(InventoryReservationEvent event) {
        boolean allItemsAvailable = true;
        StringBuilder failureReason = new StringBuilder();

        for (OrderItemDto item : event.getItems()) {
            Optional<InventoryItem> inventoryItemOpt = inventoryRepository.findItemById(item.getProductId());

            if (inventoryItemOpt.isEmpty()) {
                allItemsAvailable = false;
                failureReason.append("Product not found: ").append(item.getProductName()).append(". ");
            } else {
                InventoryItem inventoryItem = inventoryItemOpt.get();
                int reservedAcrossOrders = inventoryItem.getReservedQuantity();
                int available = inventoryItem.getAvailableQuantity();

                if (available - reservedAcrossOrders < item.getQuantity()) {
                    allItemsAvailable = false;
                    failureReason.append("Insufficient inventory for product ")
                            .append(item.getProductName())
                            .append(". Requested: ").append(item.getQuantity())
                            .append(", Available: ").append(available - reservedAcrossOrders)
                            .append(". ");
                }
            }
        }

        if (allItemsAvailable) {
            createOrUpdateReservation(event);
        } else {
            sendFailureResponse(event, failureReason.toString());
        }
    }

    private void createOrUpdateReservation(InventoryReservationEvent event) {
        Reservation reservation = new Reservation();
        reservation.setOrderId(event.getOrderId());
        reservation.setItems(event.getItems());
        reservation.setReservationTime(LocalDateTime.now());
        reservation.setExpiryTime(LocalDateTime.now().plusMinutes(15));

        for (OrderItemDto item : event.getItems()) {
            inventoryRepository.findItemById(item.getProductId()).ifPresent(inventoryItem -> {
                int updatedReserved = inventoryItem.getReservedQuantity() + item.getQuantity();
                int updatedAvailable = inventoryItem.getAvailableQuantity();
                inventoryRepository.updateItemQuantity(item.getProductId(), updatedAvailable, updatedReserved);
            });
        }

        inventoryRepository.saveReservation(reservation);

        InventoryReservationEvent responseEvent = InventoryReservationEvent.builder()
                .orderId(event.getOrderId())
                .items(event.getItems())
                .status("SUCCESS")
                .build();

        eventProducer.sendReservationResponse(responseEvent);
        System.out.println("Inventory reserved for 15 minutes for order: " + event.getOrderId());
    }

    private void sendFailureResponse(InventoryReservationEvent event, String reason) {
        InventoryReservationEvent responseEvent = InventoryReservationEvent.builder()
                .orderId(event.getOrderId())
                .items(event.getItems())
                .status("FAILED")
                .reason(reason)
                .build();

        eventProducer.sendReservationResponse(responseEvent);
        System.out.println("Inventory reservation failed: " + reason);
    }

    @Transactional
    public void releaseReservation(Long orderId) {
        inventoryRepository.findReservationById(orderId)
                .ifPresent(reservation -> {
                    for (OrderItemDto item : reservation.getItems()) {
                        inventoryRepository.findItemById(item.getProductId()).ifPresent(inventoryItem -> {
                            int updatedReserved = inventoryItem.getReservedQuantity() - item.getQuantity();
                            int updatedAvailable = inventoryItem.getAvailableQuantity();
                            inventoryRepository.updateItemQuantity(item.getProductId(), updatedAvailable, updatedReserved);
                        });
                    }
                    inventoryRepository.deleteReservation(orderId);
                });
    }

    @Transactional
    public void handleOrderCompletion(OrderCompletionEvent event) {
        if ("COMPLETED".equals(event.getStatus())) {
            confirmReservation(event.getOrderId());
        } else if ("FAILED".equals(event.getStatus())) {
            releaseReservation(event.getOrderId());
            System.out.println("Inventory released for failed order: " + event.getOrderId());
        }
    }

    private void confirmReservation(Long orderId) {
        inventoryRepository.findReservationById(orderId)
                .ifPresent(reservation -> {
                    for (OrderItemDto item : reservation.getItems()) {
                        inventoryRepository.findItemById(item.getProductId()).ifPresent(inventoryItem -> {
                            int newReserved = inventoryItem.getReservedQuantity() - item.getQuantity();
                            inventoryRepository.updateItemQuantity(item.getProductId(), inventoryItem.getAvailableQuantity(), newReserved);
                        });
                    }
                    inventoryRepository.deleteReservation(orderId);
                    System.out.println("Inventory confirmed for completed order: " + orderId);
                });
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkExpiredReservations() {
        List<Reservation> expiredReservations = inventoryRepository.findExpiredReservations();

        for (Reservation reservation : expiredReservations) {
            Long orderId = reservation.getOrderId();
            System.out.println("Reservation expired for order: " + orderId);

            releaseReservation(orderId);

            OrderCompletionEvent completionEvent = OrderCompletionEvent.builder()
                    .orderId(orderId)
                    .status("FAILED")
                    .reason("Reservation expired")
                    .build();

            eventProducer.sendReservationExpiration(completionEvent);
        }
    }
}
