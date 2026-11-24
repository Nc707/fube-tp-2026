package com.tiendaonline.pedidos.exception;

public abstract class RecursoNoEncontradoException extends RuntimeException {

    protected RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
