package com.javaSaga.events.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCompletionEvent {
    private Long orderId;
    private String status; // COMPLETED, FAILED
    private String reason; // If FAILED
}
