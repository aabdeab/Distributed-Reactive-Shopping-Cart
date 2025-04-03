package com.javaSaga.events.Events;

import com.javaSaga.events.DTOs.OrderItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryReservationEvent {
    private Long orderId;
    private List<OrderItemDto> items;
    private String status; // "SUCCESS", "FAILED", or "RELEASE" for compensating transactions
    private String reason; // Filled when status is "FAILED" to explain why
}