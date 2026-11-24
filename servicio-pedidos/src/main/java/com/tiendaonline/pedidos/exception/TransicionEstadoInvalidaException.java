package com.tiendaonline.pedidos.exception;

import com.tiendaonline.pedidos.domain.EstadoPedido;
import lombok.Getter;

@Getter
public class TransicionEstadoInvalidaException extends RuntimeException {

    private final EstadoPedido estadoActual;
    private final EstadoPedido estadoDestino;

    public TransicionEstadoInvalidaException(Long pedidoId, EstadoPedido estadoActual, EstadoPedido estadoDestino) {
        super("El pedido " + pedidoId + " no puede pasar de " + estadoActual + " a " + estadoDestino);
        this.estadoActual = estadoActual;
        this.estadoDestino = estadoDestino;
    }
}
