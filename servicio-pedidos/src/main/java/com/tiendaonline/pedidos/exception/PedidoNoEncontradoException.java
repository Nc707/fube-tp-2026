package com.tiendaonline.pedidos.exception;

public class PedidoNoEncontradoException extends RecursoNoEncontradoException {

    public PedidoNoEncontradoException(Long pedidoId) {
        super("No existe el pedido con id " + pedidoId);
    }
}
