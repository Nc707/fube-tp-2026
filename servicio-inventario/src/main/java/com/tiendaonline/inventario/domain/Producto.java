package com.tiendaonline.inventario.domain;

import com.tiendaonline.inventario.exception.StockInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "productos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible;

    public Producto(Long id, String sku, String nombre, BigDecimal precio, int cantidadDisponible) {
        this.id = id;
        this.sku = sku;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidadDisponible = cantidadDisponible;
    }

    public boolean tieneStock(int cantidad) {
        return cantidadDisponible >= cantidad;
    }

    /** Descuenta stock disponible; falla si no alcanza. */
    public void descontar(int cantidad) {
        validarPositiva(cantidad);
        if (!tieneStock(cantidad)) {
            throw new StockInsuficienteException(
                    List.of(new StockInsuficienteException.Faltante(id, cantidad, cantidadDisponible)));
        }
        cantidadDisponible -= cantidad;
    }

    /** Devuelve unidades al stock disponible (por ejemplo, al liberar una reserva). */
    public void reponer(int cantidad) {
        validarPositiva(cantidad);
        cantidadDisponible += cantidad;
    }

    private static void validarPositiva(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
    }
}
