package com.tiendaonline.pedidos.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CrearPedidoRequest(
        @NotNull @Positive Long clienteId,
        @NotEmpty @Valid List<@NotNull Item> items) {

    public record Item(
            @NotNull @Positive Long productoId,
            @NotNull @Positive Integer cantidad) {
    }
}
