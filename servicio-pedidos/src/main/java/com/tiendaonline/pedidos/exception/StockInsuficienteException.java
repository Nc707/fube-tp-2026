package com.tiendaonline.pedidos.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class StockInsuficienteException extends RuntimeException {

    public record Faltante(Long productoId, int cantidadSolicitada, int cantidadDisponible) {
    }

    private final List<Faltante> faltantes;

    public StockInsuficienteException(List<Faltante> faltantes) {
        super("Stock insuficiente para " + faltantes.size() + " producto(s)");
        this.faltantes = List.copyOf(faltantes);
    }
}
