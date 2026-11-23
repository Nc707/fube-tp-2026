package com.tiendaonline.inventario.exception;

public class ReservaNoEncontradaException extends RecursoNoEncontradoException {

    public ReservaNoEncontradaException(Long reservaId) {
        super("No existe la reserva con id " + reservaId);
    }
}
