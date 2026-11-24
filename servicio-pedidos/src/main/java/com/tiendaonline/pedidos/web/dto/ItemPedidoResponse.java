package com.tiendaonline.pedidos.web.dto;

import com.tiendaonline.pedidos.domain.ItemPedido;
import java.math.BigDecimal;

public record ItemPedidoResponse(Long id, Long productoId, int cantidad, BigDecimal precioUnitario,
                                 BigDecimal subtotal) {

    public static ItemPedidoResponse de(ItemPedido item) {
        return new ItemPedidoResponse(item.getId(), item.getProductoId(), item.getCantidad(),
                item.getPrecioUnitario(), item.subtotal());
    }
}
