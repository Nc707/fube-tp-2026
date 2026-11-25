package com.tiendaonline.pedidos.client;

import java.math.BigDecimal;

public record ReservaInventario(Long id, Long productoId, int cantidad, BigDecimal precioUnitario) {
}
