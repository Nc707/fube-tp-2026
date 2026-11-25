package com.tiendaonline.pedidos.exception;

public class ProductoNoEncontradoException extends RecursoNoEncontradoException {

    public ProductoNoEncontradoException(String detalle) {
        super(detalle);
    }
}
