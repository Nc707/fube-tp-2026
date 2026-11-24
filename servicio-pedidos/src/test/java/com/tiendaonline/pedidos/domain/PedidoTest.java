package com.tiendaonline.pedidos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tiendaonline.pedidos.exception.TransicionEstadoInvalidaException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PedidoTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final LocalDateTime LATER = NOW.plusHours(1);

    @Test
    void crearIniciaEnCreadoSinItemsYConTotalCero() {
        Pedido pedido = Pedido.crear(5L, NOW);

        assertThat(pedido.getClienteId()).isEqualTo(5L);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(pedido.getMontoTotal()).isEqualByComparingTo("0");
        assertThat(pedido.getItems()).isEmpty();
        assertThat(pedido.getFechaCreacion()).isEqualTo(NOW);
        assertThat(pedido.getFechaActualizacion()).isEqualTo(NOW);
    }

    @Test
    void crearSinClienteFalla() {
        assertThatThrownBy(() -> Pedido.crear(null, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void agregarItemsCalculaElMontoTotal() {
        Pedido pedido = Pedido.crear(5L, NOW);

        pedido.agregarItem(1L, 2, new BigDecimal("45000.00"));
        pedido.agregarItem(2L, 3, new BigDecimal("18500.50"));

        assertThat(pedido.getItems()).hasSize(2);
        assertThat(pedido.getMontoTotal()).isEqualByComparingTo("145501.50");
    }

    @Test
    void agregarItemGuardaLosDatosYLaReferenciaAlPedido() {
        Pedido pedido = Pedido.crear(5L, NOW);

        ItemPedido item = pedido.agregarItem(1L, 2, new BigDecimal("100.00"));

        assertThat(item.getPedido()).isSameAs(pedido);
        assertThat(item.getProductoId()).isEqualTo(1L);
        assertThat(item.getCantidad()).isEqualTo(2);
        assertThat(item.getPrecioUnitario()).isEqualByComparingTo("100.00");
        assertThat(item.subtotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void agregarItemConDatosInvalidosFalla() {
        Pedido pedido = Pedido.crear(5L, NOW);

        assertThatThrownBy(() -> pedido.agregarItem(1L, 0, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pedido.agregarItem(1L, 1, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pedido.agregarItem(null, 1, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(pedido.getItems()).isEmpty();
    }

    @Test
    void noSePuedenAgregarItemsSiElPedidoNoEstaCreado() {
        Pedido pedido = Pedido.crear(5L, NOW);
        pedido.confirmar(LATER);

        assertThatThrownBy(() -> pedido.agregarItem(1L, 1, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void losItemsNoSePuedenModificarDesdeAfuera() {
        Pedido pedido = Pedido.crear(5L, NOW);
        pedido.agregarItem(1L, 1, BigDecimal.TEN);

        assertThatThrownBy(() -> pedido.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void confirmarDesdeCreadoActualizaEstadoYFecha() {
        Pedido pedido = Pedido.crear(5L, NOW);

        pedido.confirmar(LATER);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(pedido.getFechaActualizacion()).isEqualTo(LATER);
        assertThat(pedido.getFechaCreacion()).isEqualTo(NOW);
    }

    @Test
    void confirmarUnPedidoYaConfirmadoFalla() {
        Pedido pedido = Pedido.crear(5L, NOW);
        pedido.confirmar(LATER);

        assertThatThrownBy(() -> pedido.confirmar(LATER))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void cancelarDesdeCreado() {
        Pedido pedido = Pedido.crear(5L, NOW);

        pedido.cancelar(LATER);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(pedido.getFechaActualizacion()).isEqualTo(LATER);
    }

    @Test
    void cancelarDesdeConfirmado() {
        Pedido pedido = Pedido.crear(5L, NOW);
        pedido.confirmar(NOW);

        pedido.cancelar(LATER);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
    }

    @Test
    void cancelarUnPedidoCanceladoOExpiradoFallaSinCambiarElEstado() {
        Pedido cancelado = Pedido.crear(5L, NOW);
        cancelado.cancelar(NOW);
        Pedido expirado = Pedido.crear(5L, NOW);
        expirado.expirar(NOW);

        assertThatThrownBy(() -> cancelado.cancelar(LATER))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
        assertThatThrownBy(() -> expirado.cancelar(LATER))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
        assertThat(cancelado.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(expirado.getEstado()).isEqualTo(EstadoPedido.EXPIRADO);
        assertThat(cancelado.getFechaActualizacion()).isEqualTo(NOW);
    }

    @Test
    void expirarDesdeCreado() {
        Pedido pedido = Pedido.crear(5L, NOW);

        pedido.expirar(LATER);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EXPIRADO);
        assertThat(pedido.getFechaActualizacion()).isEqualTo(LATER);
    }

    @Test
    void soloSePuedeExpirarUnPedidoCreado() {
        Pedido confirmado = Pedido.crear(5L, NOW);
        confirmado.confirmar(NOW);

        assertThatThrownBy(() -> confirmado.expirar(LATER))
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("CONFIRMADO")
                .hasMessageContaining("EXPIRADO");
    }
}
