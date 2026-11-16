
package com.shopease.shopease.controller;

import com.shopease.shopease.dto.OrderResponse;
import com.shopease.shopease.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(Authentication authentication) {
        OrderResponse response = orderService.placeOrder(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<OrderResponse> myOrders(Authentication authentication) {
        return orderService.getMyOrders(authentication.getName());
    }

    @GetMapping("/{id}")
    public OrderResponse myOrder(Authentication authentication, @PathVariable Long id) {
        return orderService.getMyOrder(authentication.getName(), id);
    }
}