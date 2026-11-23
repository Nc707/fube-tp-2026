package com.tiendaonline.inventario.exception;

public class ReservaYaLiberadaException extends RuntimeException {

    public ReservaYaLiberadaException(Long reservaId) {
        super("La reserva " + reservaId + " ya se encuentra LIBERADA");
    }
}
