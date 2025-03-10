package com.javaSaga.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentEvent {
    private Long orderId;
    private Long userId;
    private double amount;
    private String status; // SUCCESS or FAILED
    private String transactionId;
    private String reason; // If FAILED
}
