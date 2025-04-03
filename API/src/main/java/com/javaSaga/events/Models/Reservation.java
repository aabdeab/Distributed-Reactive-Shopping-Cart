package com.javaSaga.events.Models;

import com.javaSaga.events.DTOs.OrderItemDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class Reservation {
    private Long orderId;
    private List<OrderItemDto> items;
    private LocalDateTime reservationTime;
    private LocalDateTime expiryTime;
}