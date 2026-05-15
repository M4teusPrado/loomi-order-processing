package com.loomi.orderprocessing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Item a ser incluído no pedido")
public record OrderItemRequest(
        @Schema(description = "Identificador do produto", example = "LAPTOP-PRO-2024")
        @NotBlank String productId,

        @Schema(description = "Quantidade desejada", example = "1", minimum = "1")
        @Min(1) int quantity
) {}
