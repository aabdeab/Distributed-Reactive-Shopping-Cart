package com.javaSaga.Service;
import com.javaSaga.events.Models.Cart;
import com.javaSaga.events.Models.Product;
import com.javaSaga.events.Events.OrderCreationEvent;
import com.javaSaga.events.DTOs.OrderItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements Cartinterface {

    private final Cart cart = new Cart();

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Override
    public Mono<Void> addProduct(Product product) {
        cart.getProducts().add(product);
        return Mono.empty();
    }

    @Override
    public Mono<Cart> getCart() {
        return Mono.just(cart);
    }

    @Override
    public Mono<Void> removeProduct(Long productId) {
        cart.getProducts().removeIf(product -> product.getId().equals(productId));
        return Mono.empty();
    }

}