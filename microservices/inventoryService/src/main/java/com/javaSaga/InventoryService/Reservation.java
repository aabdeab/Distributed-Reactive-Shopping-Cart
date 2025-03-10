package com.javaSaga.InventoryService;

import com.javaSaga.events.OrderItemDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {
    private Long orderId;
    private List<OrderItemDto> items;
    private LocalDateTime reservationTime;
    private LocalDateTime expiryTime;
}
