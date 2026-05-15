package com.loomi.orderprocessing.controller;

import com.loomi.orderprocessing.dto.CreateOrderRequest;
import com.loomi.orderprocessing.dto.OrderResponse;
import com.loomi.orderprocessing.dto.OrderSummaryResponse;
import com.loomi.orderprocessing.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Endpoints de gerenciamento de pedidos")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar novo pedido", description = "Cria um pedido com um ou mais itens. O processamento ocorre de forma assíncrona via Kafka.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com status PENDING"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "422", description = "Produto indisponível")
    })
    public OrderResponse createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Buscar pedido por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    public OrderResponse getOrder(
            @Parameter(description = "UUID do pedido") @PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping
    @Operation(summary = "Listar pedidos por cliente")
    @ApiResponse(responseCode = "200", description = "Lista de pedidos do cliente")
    public List<OrderSummaryResponse> getOrdersByCustomer(
            @Parameter(description = "Identificador do cliente", required = true) @RequestParam String customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }
}
