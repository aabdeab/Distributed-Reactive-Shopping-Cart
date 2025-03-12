package com.javaSaga.Service;
import com.javaSaga.UtilsClasses.Cart;
import com.javaSaga.UtilsClasses.Product;
import com.javaSaga.events.OrderCreationEvent;
import com.javaSaga.events.OrderItemDto;
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

    @Override
    public Mono<Void> checkout() {
        System.out.println("Initiating checkout process...");

        if (cart.getProducts().isEmpty()) {
            return Mono.error(new RuntimeException("Cannot checkout with empty cart"));
        }

        // Calculate total amount
        double totalAmount = cart.getProducts().stream()
                .mapToDouble(Product::getPrice)
                .sum();

        // Convert cart items to order items
        List<OrderItemDto> orderItems = cart.getProducts().stream()
                .map(p -> OrderItemDto.builder()
                        .productId(p.getId())
                        .productName(p.getName())
                        .price(p.getPrice())
                        .quantity(1) // Assuming quantity 1 for simplicity
                        .build())
                .collect(Collectors.toList());

        // Create order creation event
        OrderCreationEvent orderEvent = OrderCreationEvent.builder()
                .userId(cart.getUserId())
                .items(orderItems)
                .status("PENDING")
                .totalAmount(totalAmount)
                .build();

        // Send HTTP request to OrderService to create order
        return webClientBuilder.build()
                .post()
                .uri("http://localhost:8081/api/orders")
                .body(Mono.just(orderEvent), OrderCreationEvent.class)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    System.out.println("Order creation initiated: " + response);
                    cart.getProducts().clear(); // Clear cart after successful order creation
                })
                .doOnError(error -> System.err.println("Failed to create order: " + error.getMessage()))
                .then();
    }
}