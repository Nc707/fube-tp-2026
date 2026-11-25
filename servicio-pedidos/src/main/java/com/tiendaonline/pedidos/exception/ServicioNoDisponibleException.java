package com.tiendaonline.pedidos.exception;

import lombok.Getter;

@Getter
public class ServicioNoDisponibleException extends RuntimeException {

    private final String servicio;

    public ServicioNoDisponibleException(String servicio, String detalle, Throwable causa) {
        super("El servicio " + servicio + " no esta disponible: " + detalle, causa);
        this.servicio = servicio;
    }
}
