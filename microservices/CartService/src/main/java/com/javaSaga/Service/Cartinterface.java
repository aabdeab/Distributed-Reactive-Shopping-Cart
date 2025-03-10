package com.javaSaga.Service;

import reactor.core.publisher.Mono;

public interface Cartinterface {

    Mono<Void> addProduct(Product product);

    Mono<Cart> getCart();

    Mono<Void> removeProduct(Long productId);

    Mono<Void> checkout();
}
