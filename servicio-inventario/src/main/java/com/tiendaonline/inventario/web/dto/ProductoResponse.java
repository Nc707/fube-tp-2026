package com.tiendaonline.inventario.web.dto;

import com.tiendaonline.inventario.domain.Producto;
import java.math.BigDecimal;

public record ProductoResponse(Long id, String sku, String nombre, BigDecimal precio, int cantidadDisponible) {

    public static ProductoResponse de(Producto producto) {
        return new ProductoResponse(producto.getId(), producto.getSku(), producto.getNombre(),
                producto.getPrecio(), producto.getCantidadDisponible());
    }
}
