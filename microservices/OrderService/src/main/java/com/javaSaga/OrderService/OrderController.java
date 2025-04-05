package com.javaSaga.OrderService;
import com.javaSaga.events.*;
import jakarta.validation.constraints.Null;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;


    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    @PostMapping("/checkout")
    public Mono<String> createOrder(){
        return orderService.checkout().thenReturn("Commande en cours de traitement");

    }
} 