package com.javaSaga.PaymentService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class Payment {
    private Long orderId;
    private Long userId;
    private double amount;
    private String transactionId;
    private String status;
    private LocalDateTime timestamp;
    private String refundReason;
    private LocalDateTime refundTimestamp;
}