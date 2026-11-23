package com.tiendaonline.inventario.exception;

public class ProductoNoEncontradoException extends RecursoNoEncontradoException {

    public ProductoNoEncontradoException(Long productoId) {
        super("No existe el producto con id " + productoId);
    }
}
