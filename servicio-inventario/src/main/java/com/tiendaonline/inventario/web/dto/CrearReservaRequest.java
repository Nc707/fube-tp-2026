package com.tiendaonline.inventario.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "Solicitud de reserva de stock para todos los items de un pedido (todo o nada)")
public record CrearReservaRequest(
        @NotNull @Positive Long pedidoId,
        @NotEmpty @Valid List<@NotNull Item> items) {

    public record Item(
            @NotNull @Positive Long productoId,
            @NotNull @Positive Integer cantidad) {
    }
}
