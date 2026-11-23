package com.tiendaonline.inventario.domain;

import com.tiendaonline.inventario.exception.ReservaYaLiberadaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;

    /**
     * Crea una reserva ACTIVA descontando las unidades del stock del producto.
     * El precio unitario queda "congelado" al momento de reservar.
     */
    public static Reserva crear(Producto producto, Long pedidoId, int cantidad, LocalDateTime ahora) {
        producto.descontar(cantidad);
        Reserva reserva = new Reserva();
        reserva.producto = producto;
        reserva.pedidoId = pedidoId;
        reserva.cantidad = cantidad;
        reserva.precioUnitario = producto.getPrecio();
        reserva.estado = EstadoReserva.ACTIVA;
        reserva.fechaCreacion = ahora;
        return reserva;
    }

    /** Libera la reserva devolviendo las unidades al stock del producto. */
    public void liberar(LocalDateTime ahora) {
        if (!estaActiva()) {
            throw new ReservaYaLiberadaException(id);
        }
        producto.reponer(cantidad);
        estado = EstadoReserva.LIBERADA;
        fechaLiberacion = ahora;
    }

    public boolean estaActiva() {
        return estado == EstadoReserva.ACTIVA;
    }
}
