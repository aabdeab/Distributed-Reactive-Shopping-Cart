package com.javaSaga.Service;

import com.javaSaga.UtilsClasses.Cart;
import com.javaSaga.UtilsClasses.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
                .thenReturn("Produit retiré du panier");
    }

    @PostMapping("/checkout")
    public Mono<String> checkout() {
        return cartService.checkout()
                .thenReturn("Commande en cours de traitement");
    }
}
