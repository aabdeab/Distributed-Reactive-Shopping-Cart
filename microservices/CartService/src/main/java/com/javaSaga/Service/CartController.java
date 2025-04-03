package com.javaSaga.Service;

import com.javaSaga.Exceptions.ProductNotFoundException;
import com.javaSaga.events.Models.Cart;
import com.javaSaga.events.Models.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin("*")
public class CartController {

    @Autowired
    private CartServiceImpl cartService;

    @PostMapping("/add")
    public Mono<String> addProductToCart(@RequestBody Product product) {
        return cartService.addProduct(product)
                .thenReturn("Produit ajouté au panier");
    }

    @GetMapping
    public Mono<Cart> getCart() {
        return cartService.getCart();
    }

    @DeleteMapping("/remove/{productId}")
    public Mono<String> removeProductFromCart(@PathVariable Long productId) {
        return cartService.removeProduct(productId)
                .thenReturn("Produit retiré du panier")
                .onErrorResume(e -> Mono.error(new ProductNotFoundException("Product not found")));
    }


    @PostMapping("/checkout")
    public Mono<String> checkout() {
        return cartService.checkout()
                .thenReturn("Commande en cours de traitement");
    }
    @DeleteMapping
    public Mono<String> removeProducts() {
        return cartService.getCart()
                .flatMap(cart -> {
                    cart.setProducts(new ArrayList<>());
                    return cartService.getCart();
                })
                .thenReturn("Cart is cleared with success");
    }
}
