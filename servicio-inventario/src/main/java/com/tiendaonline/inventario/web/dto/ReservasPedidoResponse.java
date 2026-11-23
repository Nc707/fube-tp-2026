package com.tiendaonline.inventario.web.dto;

import com.tiendaonline.inventario.domain.Reserva;
import java.util.List;

public record ReservasPedidoResponse(Long pedidoId, List<ReservaResponse> reservas) {

    public static ReservasPedidoResponse de(Long pedidoId, List<Reserva> reservas) {
        return new ReservasPedidoResponse(pedidoId, reservas.stream().map(ReservaResponse::de).toList());
    }
}
