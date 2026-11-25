package com.tiendaonline.pedidos.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tiendaonline.pedidos.exception.TransicionEstadoInvalidaException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PedidoTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final LocalDateTime LATER = NOW.plusHours(1);

    @Test
    void crearIniciaEnCreadoSinItemsYConTotalCero() {
        Pedido pedido = Pedido.crear(5L, NOW);

        assertEquals(5L, pedido.getClienteId());
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        assertEquals(BigDecimal.ZERO, pedido.getMontoTotal());
        assertTrue(pedido.getItems().isEmpty());
        assertEquals(NOW, pedido.getFechaCreacion());
        assertEquals(NOW, pedido.getFechaActualizacion());
    }

    @Test
    void crearSinClienteFalla() {
        assertThrows(IllegalArgumentException.class, () -> Pedido.crear(null, NOW));
    }

    @Test
    void agregarItemsCalculaElMontoTotal() {
        Pedido pedido = Pedido.crear(5L, NOW);

        pedido.agregarItem(1L, 2, new BigDecimal("45000.00"));
        pedido.agregarItem(2L, 3, new BigDecimal("18500.50"));

        assertEquals(2, pedido.getItems().size());
        assertEquals(new BigDecimal("145501.50"), pedido.getMontoTotal());
    }

    @Test
    void agregarItemGuardaLosDatosYLaReferenciaAlPedido() {
        Pedido pedido = Pedido.crear(5L, NOW);

        ItemPedido item = pedido.agregarItem(1L, 2, new BigDecimal("100.00"));

        assertSame(pedido, item.getPedido());
        assertEquals(1L, item.getProductoId());
        assertEquals(2, item.getCantidad());
        assertEquals(new BigDecimal("100.00"), item.getPrecioUnitario());
        assertEquals(new BigDecimal("200.00"), item.subtotal());
    }

    // productoId vacio = null
    @ParameterizedTest
    @CsvSource({
            "1, 0, 10",
            "1, -1, 10",
            "1, 1, -1",
            ", 1, 10"
    })
    void agregarItemConDatosInvalidosFalla(Long productoId, int cantidad, BigDecimal precioUnitario) {
        Pedido pedido = Pedido.crear(5L, NOW);

        assertThrows(IllegalArgumentException.class,
                () -> pedido.agregarItem(productoId, cantidad, precioUnitario));
        assertTrue(pedido.getItems().isEmpty());
    }

    @Test
    void noSePuedenAgregarItemsSiElPedidoNoEstaCreado() {
        Pedido pedido = Pedido.crear(5L, NOW);
        pedido.confirmar(LATER);

        assertThrows(IllegalStateException.class, () -> pedido.agregarItem(1L, 1, BigDecimal.TEN));
    }

    @Test
    void losItemsNoSePuedenModificarDesdeAfuera() {
        Pedido pedido = Pedido.crear(5L, NOW);
        pedido.agregarItem(1L, 1, BigDecimal.TEN);

        assertThrows(UnsupportedOperationException.class, () -> pedido.getItems().clear());
    }

    @Test
    void confirmarDesdeCreadoActualizaEstadoYFecha() {
        Pedido pedido = Pedido.crear(5L, NOW);

        pedido.confirmar(LATER);

        assertEquals(EstadoPedido.CONFIRMADO, pedido.getEstado());
        assertEquals(LATER, pedido.getFechaActualizacion());
        assertEquals(NOW, pedido.getFechaCreacion());
    }

    @Test
    void confirmarUnPedidoYaConfirmadoFalla() {
        Pedido pedido = Pedido.crear(5L, NOW);
        pedido.confirmar(LATER);

        assertThrows(TransicionEstadoInvalidaException.class, () -> pedido.confirmar(LATER));
    }

    @Test
    void cancelarDesdeCreado() {
        Pedido pedido = Pedido.crear(5L, NOW);

        pedido.cancelar(LATER);

        assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
        assertEquals(LATER, pedido.getFechaActualizacion());
    }

    @Test
    void cancelarDesdeConfirmado() {
        Pedido pedido = Pedido.crear(5L, NOW);
        pedido.confirmar(NOW);

        pedido.cancelar(LATER);

        assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
    }

    @Test
    void cancelarUnPedidoCanceladoOExpiradoFallaSinCambiarElEstado() {
        Pedido cancelado = Pedido.crear(5L, NOW);
        cancelado.cancelar(NOW);
        Pedido expirado = Pedido.crear(5L, NOW);
        expirado.expirar(NOW);

        assertThrows(TransicionEstadoInvalidaException.class, () -> cancelado.cancelar(LATER));
        assertThrows(TransicionEstadoInvalidaException.class, () -> expirado.cancelar(LATER));
        assertEquals(EstadoPedido.CANCELADO, cancelado.getEstado());
        assertEquals(EstadoPedido.EXPIRADO, expirado.getEstado());
        assertEquals(NOW, cancelado.getFechaActualizacion());
    }

    @Test
    void expirarDesdeCreado() {
        Pedido pedido = Pedido.crear(5L, NOW);

        pedido.expirar(LATER);

        assertEquals(EstadoPedido.EXPIRADO, pedido.getEstado());
        assertEquals(LATER, pedido.getFechaActualizacion());
    }

    @Test
    void soloSePuedeExpirarUnPedidoCreado() {
        Pedido confirmado = Pedido.crear(5L, NOW);
        confirmado.confirmar(NOW);

        TransicionEstadoInvalidaException ex = assertThrows(TransicionEstadoInvalidaException.class,
                () -> confirmado.expirar(LATER));
        assertTrue(ex.getMessage().contains("CONFIRMADO"));
        assertTrue(ex.getMessage().contains("EXPIRADO"));
    }
}
