package com.tiendaonline.inventario.web.dto;

import com.tiendaonline.inventario.domain.EstadoReserva;
import com.tiendaonline.inventario.domain.Reserva;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long pedidoId,
        Long productoId,
        int cantidad,
        BigDecimal precioUnitario,
        EstadoReserva estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaLiberacion) {

    public static ReservaResponse de(Reserva reserva) {
        return new ReservaResponse(reserva.getId(), reserva.getPedidoId(), reserva.getProducto().getId(),
                reserva.getCantidad(), reserva.getPrecioUnitario(), reserva.getEstado(),
                reserva.getFechaCreacion(), reserva.getFechaLiberacion());
    }
}
