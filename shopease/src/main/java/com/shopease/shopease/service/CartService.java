package com.shopease.shopease.service;

import com.shopease.shopease.dto.AddToCartRequest;
import com.shopease.shopease.dto.CartItemResponse;
import com.shopease.shopease.dto.CartResponse;
import com.shopease.shopease.dto.UpdateCartItemRequest;
import com.shopease.shopease.entity.Cart;
import com.shopease.shopease.entity.CartItem;
import com.shopease.shopease.entity.Product;
import com.shopease.shopease.entity.User;
import com.shopease.shopease.repository.CartRepository;
import com.shopease.shopease.repository.ProductRepository;
import com.shopease.shopease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CartResponse getCart(String email) {
        return toResponse(getOrCreateCart(email));
    }

    @Transactional
    public CartResponse addItem(String email, AddToCartRequest request) {
        Cart cart = getOrCreateCart(email);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int newQuantity = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        checkStock(product, newQuantity);

        if (existing != null) {
            existing.setQuantity(newQuantity);
        } else {
            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(newQuantity)
                    .build());
        }

        cartRepository.saveAndFlush(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(String email, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(email);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        checkStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String email, Long itemId) {
        Cart cart = getOrCreateCart(email);

        boolean removed = cart.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }

        return toResponse(cart);
    }

    private Cart getOrCreateCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private void checkStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only " + product.getStock() + " items in stock");
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> new CartItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getPrice(),
                        item.getQuantity(),
                        item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), items, total);
    }
}