package com.javaSaga.Service;

import com.javaSaga.events.Models.Cart;
import com.javaSaga.events.Models.Product;
import reactor.core.publisher.Mono;

public interface Cartinterface {

    Mono<Void> addProduct(Product product);

    Mono<Cart> getCart();

    Mono<Void> removeProduct(Long productId);

}
