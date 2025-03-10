package com.javaSaga.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCreationEvent {
    private Long orderId;
    private Long userId;
    private List<OrderItemDto> items;
    private String status;
    private double totalAmount;
}