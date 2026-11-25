package com.tiendaonline.pedidos.exception;

public class ClienteNoEncontradoException extends RecursoNoEncontradoException {

    public ClienteNoEncontradoException(Long clienteId) {
        super("No existe el cliente con id " + clienteId);
    }
}
