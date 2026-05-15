package com.loomi.orderprocessing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Requisição para criação de um novo pedido")
public record CreateOrderRequest(
        @Schema(description = "Identificador do cliente", example = "customer-123")
        @NotBlank String customerId,

        @Schema(description = "Lista de itens do pedido")
        @NotEmpty @Valid List<OrderItemRequest> items
) {}
