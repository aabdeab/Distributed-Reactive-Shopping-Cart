package com.javaSaga;

import com.javaSaga.Service.CartController;
import com.javaSaga.Service.CartServiceImpl;
import com.javaSaga.events.Models.Cart;
import com.javaSaga.events.Models.Product;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

@WebFluxTest(CartController.class)
public class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartServiceImpl cartService;

    @Test
    void addProductToCart_shouldReturnSuccessMessage() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Coffee");

        Mockito.when(cartService.addProduct(product)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/cart/add")
                .bodyValue(product)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Produit ajouté au panier");
    }

    @Test
    void getCart_shouldReturnCart() {
        Cart cart = new Cart();
        cart.setProducts(Collections.emptyList());

        Mockito.when(cartService.getCart()).thenReturn(Mono.just(cart));

        webTestClient.get()
                .uri("/api/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Cart.class)
                .isEqualTo(cart);
    }

    @Test
    void removeProduct_shouldReturnSuccessMessage() {
        Long productId = 1L;

        Mockito.when(cartService.removeProduct(productId)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/cart/remove/{id}", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Produit retiré du panier");
    }

    @Test
    void clearCart_shouldReturnSuccessMessage() {
        Cart cart = new Cart();
        cart.setProducts(Collections.emptyList());

        Mockito.when(cartService.getCart()).thenReturn(Mono.just(cart));

        webTestClient.delete()
                .uri("/api/cart")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Cart is cleared with success");
    }
}
