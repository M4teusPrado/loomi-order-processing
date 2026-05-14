package com.loomi.orderprocessing.controller;

import com.loomi.orderprocessing.dto.CreateOrderRequest;
import com.loomi.orderprocessing.dto.OrderResponse;
import com.loomi.orderprocessing.dto.OrderSummaryResponse;
import com.loomi.orderprocessing.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrdersByCustomer(@RequestParam String customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }
}
