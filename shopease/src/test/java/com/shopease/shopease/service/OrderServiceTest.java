package com.shopease.shopease.service;

import com.shopease.shopease.dto.OrderResponse;
import com.shopease.shopease.entity.Cart;
import com.shopease.shopease.entity.CartItem;
import com.shopease.shopease.entity.Order;
import com.shopease.shopease.entity.OrderItem;
import com.shopease.shopease.entity.OrderStatus;
import com.shopease.shopease.entity.Product;
import com.shopease.shopease.entity.Role;
import com.shopease.shopease.entity.User;
import com.shopease.shopease.repository.CartRepository;
import com.shopease.shopease.repository.OrderRepository;
import com.shopease.shopease.repository.ProductRepository;
import com.shopease.shopease.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String EMAIL = "ramya@test.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).name("Ramya").email(EMAIL)
                .password("x").role(Role.USER).build();

        product = Product.builder()
                .id(10L).name("iPhone")
                .price(new BigDecimal("1000.00")).stock(5).build();
    }

    @Test
    void placeOrderWithEmptyCartThrowsBadRequest() {
        Cart cart = Cart.builder().id(1L).user(user).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.placeOrder(EMAIL));

        assertEquals(400, ex.getStatusCode().value());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrderReducesStockCalculatesTotalAndClearsCart() {
        Cart cart = Cart.builder().id(1L).user(user).build();
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.placeOrder(EMAIL);

        assertEquals(3, product.getStock());
        assertEquals(OrderStatus.PLACED, response.status());
        assertEquals(new BigDecimal("2000.00"), response.totalAmount());
        assertEquals(new BigDecimal("1000.00"), response.items().get(0).priceAtPurchase());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void placeOrderWithInsufficientStockThrowsAndKeepsStock() {
        product.setStock(1);
        Cart cart = Cart.builder().id(1L).user(user).build();
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.placeOrder(EMAIL));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals(1, product.getStock());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrderThatIsNotPlacedThrowsConflict() {
        Order order = buildOrder(OrderStatus.PAID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelMyOrder(EMAIL, 5L));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void cancelPlacedOrderRestoresStock() {
        product.setStock(3);
        Order order = buildOrder(OrderStatus.PLACED);
        order.getItems().add(OrderItem.builder()
                .id(1L).order(order).product(product)
                .quantity(2).priceAtPurchase(new BigDecimal("1000.00")).build());

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));

        OrderResponse response = orderService.cancelMyOrder(EMAIL, 5L);

        assertEquals(5, product.getStock());
        assertEquals(OrderStatus.CANCELLED, response.status());
    }

    @Test
    void updateStatusWithInvalidTransitionThrowsConflict() {
        Order order = buildOrder(OrderStatus.PLACED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.updateStatus(5L, OrderStatus.DELIVERED));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    void updateStatusWithValidTransitionChangesStatus() {
        Order order = buildOrder(OrderStatus.PLACED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.updateStatus(5L, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, response.status());
    }

    private Order buildOrder(OrderStatus status) {
        return Order.builder()
                .id(5L).user(user).status(status)
                .totalAmount(new BigDecimal("2000.00"))
                .createdAt(LocalDateTime.now())
                .build();
    }
}