package com.tiendaonline.pedidos.web.dto;

import com.tiendaonline.pedidos.domain.EstadoPedido;
import com.tiendaonline.pedidos.domain.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        Long clienteId,
        EstadoPedido estado,
        BigDecimal montoTotal,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        List<ItemPedidoResponse> items) {

    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getClienteId(), pedido.getEstado(),
                pedido.getMontoTotal(), pedido.getFechaCreacion(), pedido.getFechaActualizacion(),
                pedido.getItems().stream().map(ItemPedidoResponse::de).toList());
    }
}
