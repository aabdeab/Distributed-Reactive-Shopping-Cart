package com.javaSaga.Service;

import com.javaSaga.UtilsClasses.Cart;
import com.javaSaga.UtilsClasses.Product;
import reactor.core.publisher.Mono;

public interface Cartinterface {

    Mono<Void> addProduct(Product product);

    Mono<Cart> getCart();

    Mono<Void> removeProduct(Long productId);

    Mono<Void> checkout();
}
