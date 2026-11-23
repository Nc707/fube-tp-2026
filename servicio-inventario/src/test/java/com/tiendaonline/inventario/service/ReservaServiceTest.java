package com.tiendaonline.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tiendaonline.inventario.domain.EstadoReserva;
import com.tiendaonline.inventario.domain.Producto;
import com.tiendaonline.inventario.domain.Reserva;
import com.tiendaonline.inventario.exception.PedidoConReservasActivasException;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.exception.ReservaNoEncontradaException;
import com.tiendaonline.inventario.exception.ReservaYaLiberadaException;
import com.tiendaonline.inventario.exception.SolicitudInvalidaException;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import com.tiendaonline.inventario.repository.ProductoRepository;
import com.tiendaonline.inventario.repository.ReservaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final Long PEDIDO_ID = 50L;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ProductoRepository productoRepository;

    private ReservaService reservaService;

    private Producto teclado;
    private Producto mouse;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        reservaService = new ReservaService(reservaRepository, productoRepository, clock);
        teclado = new Producto(1L, "TEC-001", "Teclado", new BigDecimal("100.00"), 10);
        mouse = new Producto(2L, "MOU-001", "Mouse", new BigDecimal("25.50"), 5);
    }

    @Test
    void reservarDescuentaStockDeTodosLosItemsYGuardaReservasActivas() {
        when(productoRepository.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(teclado, mouse));
        when(reservaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Reserva> reservas = reservaService.reservar(PEDIDO_ID,
                List.of(new ItemReserva(1L, 3), new ItemReserva(2L, 5)));

        assertThat(reservas).hasSize(2)
                .allSatisfy(r -> {
                    assertThat(r.getEstado()).isEqualTo(EstadoReserva.ACTIVA);
                    assertThat(r.getPedidoId()).isEqualTo(PEDIDO_ID);
                    assertThat(r.getFechaCreacion()).isEqualTo(AHORA);
                });
        assertThat(reservas.get(1).getPrecioUnitario()).isEqualByComparingTo("25.50");
        assertThat(teclado.getCantidadDisponible()).isEqualTo(7);
        assertThat(mouse.getCantidadDisponible()).isZero();
    }

    @Test
    void reservarConStockInsuficienteEnAlgunItemNoReservaNingunoEInformaFaltantes() {
        when(productoRepository.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(teclado, mouse));

        assertThatThrownBy(() -> reservaService.reservar(PEDIDO_ID,
                List.of(new ItemReserva(1L, 3), new ItemReserva(2L, 6))))
                .isInstanceOfSatisfying(StockInsuficienteException.class, ex ->
                        assertThat(ex.getFaltantes()).containsExactly(
                                new StockInsuficienteException.Faltante(2L, 6, 5)));

        assertThat(teclado.getCantidadDisponible()).isEqualTo(10);
        assertThat(mouse.getCantidadDisponible()).isEqualTo(5);
        verify(reservaRepository, never()).saveAll(any());
    }

    @Test
    void reservarProductoInexistenteLanzaExcepcion() {
        when(productoRepository.findAllByIdForUpdate(List.of(1L, 99L))).thenReturn(List.of(teclado));

        assertThatThrownBy(() -> reservaService.reservar(PEDIDO_ID,
                List.of(new ItemReserva(1L, 1), new ItemReserva(99L, 1))))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessageContaining("99");
        verify(reservaRepository, never()).saveAll(any());
    }

    @Test
    void reservarConProductoDuplicadoEsRechazadoSinTocarLaBase() {
        assertThatThrownBy(() -> reservaService.reservar(PEDIDO_ID,
                List.of(new ItemReserva(1L, 1), new ItemReserva(1L, 2))))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(productoRepository, reservaRepository);
    }

    @Test
    void reservarParaPedidoConReservasActivasEsRechazado() {
        when(reservaRepository.existsByPedidoIdAndEstado(PEDIDO_ID, EstadoReserva.ACTIVA)).thenReturn(true);

        assertThatThrownBy(() -> reservaService.reservar(PEDIDO_ID, List.of(new ItemReserva(1L, 1))))
                .isInstanceOf(PedidoConReservasActivasException.class);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void liberarDevuelveElStockYBloqueaElProducto() {
        Reserva reserva = Reserva.crear(teclado, PEDIDO_ID, 4, AHORA.minusHours(2));
        when(reservaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reserva));

        Reserva liberada = reservaService.liberar(10L);

        assertThat(liberada.getEstado()).isEqualTo(EstadoReserva.LIBERADA);
        assertThat(liberada.getFechaLiberacion()).isEqualTo(AHORA);
        assertThat(teclado.getCantidadDisponible()).isEqualTo(10);
        verify(productoRepository).findAllByIdForUpdate(Set.of(1L));
    }

    @Test
    void liberarReservaInexistenteLanzaExcepcion() {
        when(reservaRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.liberar(10L)).isInstanceOf(ReservaNoEncontradaException.class);
    }

    @Test
    void liberarReservaYaLiberadaLanzaExcepcion() {
        Reserva reserva = Reserva.crear(teclado, PEDIDO_ID, 4, AHORA);
        reserva.liberar(AHORA);
        when(reservaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.liberar(10L)).isInstanceOf(ReservaYaLiberadaException.class);
        assertThat(teclado.getCantidadDisponible()).isEqualTo(10);
    }

    @Test
    void liberarPorPedidoLiberaTodasLasReservasActivas() {
        Reserva r1 = Reserva.crear(teclado, PEDIDO_ID, 2, AHORA);
        Reserva r2 = Reserva.crear(mouse, PEDIDO_ID, 5, AHORA);
        when(reservaRepository.findByPedidoIdAndEstadoForUpdate(PEDIDO_ID, EstadoReserva.ACTIVA))
                .thenReturn(List.of(r1, r2));

        List<Reserva> liberadas = reservaService.liberarPorPedido(PEDIDO_ID);

        assertThat(liberadas).extracting(Reserva::getEstado).containsOnly(EstadoReserva.LIBERADA);
        assertThat(teclado.getCantidadDisponible()).isEqualTo(10);
        assertThat(mouse.getCantidadDisponible()).isEqualTo(5);
        verify(productoRepository).findAllByIdForUpdate(Set.of(1L, 2L));
    }

    @Test
    void liberarPorPedidoSinReservasActivasEsIdempotente() {
        when(reservaRepository.findByPedidoIdAndEstadoForUpdate(PEDIDO_ID, EstadoReserva.ACTIVA))
                .thenReturn(List.of());

        assertThat(reservaService.liberarPorPedido(PEDIDO_ID)).isEmpty();
        verify(productoRepository, never()).findAllByIdForUpdate(anyCollection());
    }
}
