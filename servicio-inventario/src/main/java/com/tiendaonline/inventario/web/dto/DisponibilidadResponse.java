package com.tiendaonline.inventario.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tiendaonline.inventario.domain.Producto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DisponibilidadResponse(
        Long productoId,
        String nombre,
        BigDecimal precio,
        int cantidadDisponible,
        @Schema(description = "Cantidad consultada (solo si se envio el parametro 'cantidad')")
        Integer cantidadSolicitada,
        @Schema(description = "true si alcanza el stock para la cantidad consultada (solo si se envio 'cantidad')")
        Boolean suficiente) {

    public static DisponibilidadResponse de(Producto producto, Integer cantidadSolicitada) {
        Boolean suficiente = cantidadSolicitada == null ? null : producto.tieneStock(cantidadSolicitada);
        return new DisponibilidadResponse(producto.getId(), producto.getNombre(), producto.getPrecio(),
                producto.getCantidadDisponible(), cantidadSolicitada, suficiente);
    }
}
