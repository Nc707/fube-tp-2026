package com.tiendaonline.inventario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tiendaonline.inventario.exception.ReservaYaLiberadaException;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReservaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0);

    private Producto producto(int stock) {
        return new Producto(1L, "TEC-001", "Teclado", new BigDecimal("100.00"), stock);
    }

    @Test
    void crearDescuentaStockYCongelaElPrecio() {
        Producto producto = producto(10);

        Reserva reserva = Reserva.crear(producto, 7L, 3, AHORA);

        assertThat(producto.getCantidadDisponible()).isEqualTo(7);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.ACTIVA);
        assertThat(reserva.getPedidoId()).isEqualTo(7L);
        assertThat(reserva.getCantidad()).isEqualTo(3);
        assertThat(reserva.getPrecioUnitario()).isEqualByComparingTo("100.00");
        assertThat(reserva.getFechaCreacion()).isEqualTo(AHORA);
    }

    @Test
    void crearConStockInsuficienteFallaSinModificarElStock() {
        Producto producto = producto(2);

        assertThatThrownBy(() -> Reserva.crear(producto, 7L, 3, AHORA))
                .isInstanceOf(StockInsuficienteException.class);
        assertThat(producto.getCantidadDisponible()).isEqualTo(2);
    }

    @Test
    void crearPermiteReservarExactamenteElStockDisponible() {
        Producto producto = producto(3);

        Reserva.crear(producto, 7L, 3, AHORA);

        assertThat(producto.getCantidadDisponible()).isZero();
    }

    @Test
    void liberarDevuelveElStockYMarcaLiberada() {
        Producto producto = producto(10);
        Reserva reserva = Reserva.crear(producto, 7L, 4, AHORA);

        reserva.liberar(AHORA.plusHours(1));

        assertThat(producto.getCantidadDisponible()).isEqualTo(10);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.LIBERADA);
        assertThat(reserva.getFechaLiberacion()).isEqualTo(AHORA.plusHours(1));
    }

    @Test
    void liberarDosVecesFallaYNoDuplicaElStock() {
        Producto producto = producto(10);
        Reserva reserva = Reserva.crear(producto, 7L, 4, AHORA);
        reserva.liberar(AHORA);

        assertThatThrownBy(() -> reserva.liberar(AHORA)).isInstanceOf(ReservaYaLiberadaException.class);
        assertThat(producto.getCantidadDisponible()).isEqualTo(10);
    }

    @Test
    void cantidadesNoPositivasSonRechazadas() {
        Producto producto = producto(10);

        assertThatThrownBy(() -> producto.descontar(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> producto.reponer(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
