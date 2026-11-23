package com.tiendaonline.inventario.exception;

public class PedidoConReservasActivasException extends RuntimeException {

    public PedidoConReservasActivasException(Long pedidoId) {
        super("El pedido " + pedidoId + " ya tiene reservas activas");
    }
}
