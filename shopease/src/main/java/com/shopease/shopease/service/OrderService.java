package com.shopease.shopease.service;

import com.shopease.shopease.dto.OrderItemResponse;
import com.shopease.shopease.dto.OrderResponse;
import com.shopease.shopease.entity.*;
import com.shopease.shopease.repository.CartRepository;
import com.shopease.shopease.repository.OrderRepository;
import com.shopease.shopease.repository.ProductRepository;
import com.shopease.shopease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse placeOrder(String email) {
        User user = findUser(email);

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        List<CartItem> sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();

        for (CartItem cartItem : sortedItems) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

            if (cartItem.getQuantity() > product.getStock()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Not enough stock for " + product.getName());
            }

            product.setStock(product.getStock() - cartItem.getQuantity());

            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build());

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        cart.getItems().clear();

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        User user = findUser(email);
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String email, Long orderId) {
        User user = findUser(email);
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toResponse(order);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getPriceAtPurchase(),
                        i.getPriceAtPurchase().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();

        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(),
                order.getCreatedAt(), items);
    }
}