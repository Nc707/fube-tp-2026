package com.tiendaonline.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tiendaonline.pedidos.client.ItemAReservar;
import com.tiendaonline.pedidos.client.ReservaInventario;
import com.tiendaonline.pedidos.client.ServicioClientesClient;
import com.tiendaonline.pedidos.client.ServicioInventarioClient;
import com.tiendaonline.pedidos.domain.EstadoPedido;
import com.tiendaonline.pedidos.domain.Pedido;
import com.tiendaonline.pedidos.exception.ClienteNoEncontradoException;
import com.tiendaonline.pedidos.exception.PedidoNoEncontradoException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import com.tiendaonline.pedidos.exception.StockInsuficienteException;
import com.tiendaonline.pedidos.exception.TransicionEstadoInvalidaException;
import com.tiendaonline.pedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final List<ItemAReservar> ITEMS = List.of(new ItemAReservar(1L, 2), new ItemAReservar(2L, 1));

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ServicioClientesClient clientesClient;
    @Mock
    private ServicioInventarioClient inventarioClient;
    private PedidoService pedidoService;

    @BeforeEach
    void crearServicio() {
        // reloj fijo en NOW para poder verificar las fechas
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        pedidoService = new PedidoService(pedidoRepository, clientesClient, inventarioClient, clock);
    }

    // Simula que la base le asigna el id 10 al pedido
    private void guardarAsignaId() {
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 10L);
            return pedido;
        });
    }

    @Test
    void obtenerDevuelveElPedido() {
        Pedido pedido = Pedido.crear(5L, NOW);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThat(pedidoService.obtener(1L)).isSameAs(pedido);
    }

    @Test
    void obtenerPedidoInexistenteLanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.obtener(99L))
                .isInstanceOf(PedidoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listarPorClienteDevuelveLosPedidosDelCliente() {
        List<Pedido> pedidos = List.of(Pedido.crear(5L, NOW), Pedido.crear(5L, NOW.plusHours(1)));
        when(pedidoRepository.findByClienteIdOrderByIdAsc(5L)).thenReturn(pedidos);

        assertThat(pedidoService.listarPorCliente(5L)).isEqualTo(pedidos);
    }

    @Test
    void listarPorClienteSinPedidosDevuelveListaVacia() {
        when(pedidoRepository.findByClienteIdOrderByIdAsc(7L)).thenReturn(List.of());

        assertThat(pedidoService.listarPorCliente(7L)).isEmpty();
    }

    @Test
    void crearReservaElStockYCalculaElTotalConLosPreciosDeInventario() {
        guardarAsignaId();
        when(inventarioClient.reservar(10L, ITEMS)).thenReturn(List.of(
                new ReservaInventario(1L, 1L, 2, new BigDecimal("45000.00")),
                new ReservaInventario(2L, 2L, 1, new BigDecimal("18500.00"))));

        Pedido pedido = pedidoService.crear(5L, ITEMS);

        verify(clientesClient).verificarExistencia(5L);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(pedido.getItems()).hasSize(2);
        assertThat(pedido.getMontoTotal()).isEqualByComparingTo("108500.00");
        assertThat(pedido.getFechaCreacion()).isEqualTo(NOW);
    }

    @Test
    void crearConClienteInexistenteNoGuardaNiReserva() {
        doThrow(new ClienteNoEncontradoException(5L)).when(clientesClient).verificarExistencia(5L);

        assertThatThrownBy(() -> pedidoService.crear(5L, ITEMS)).isInstanceOf(ClienteNoEncontradoException.class);

        verify(pedidoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(inventarioClient);
    }

    @Test
    void crearSinStockFallaYNoLiberaPorqueInventarioNoReservoNada() {
        guardarAsignaId();
        when(inventarioClient.reservar(anyLong(), anyList()))
                .thenThrow(new StockInsuficienteException(List.of(new StockInsuficienteException.Faltante(1L, 2, 0))));

        assertThatThrownBy(() -> pedidoService.crear(5L, ITEMS)).isInstanceOf(StockInsuficienteException.class);

        verify(inventarioClient, never()).liberarPorPedido(anyLong());
    }

    @Test
    void crearConInventarioNoDisponibleLiberaPorLasDudas() {
        guardarAsignaId();
        when(inventarioClient.reservar(anyLong(), anyList()))
                .thenThrow(new ServicioNoDisponibleException("servicio-inventario", "timeout", null));

        assertThatThrownBy(() -> pedidoService.crear(5L, ITEMS)).isInstanceOf(ServicioNoDisponibleException.class);

        verify(inventarioClient).liberarPorPedido(10L);
    }

    @Test
    void cancelarCambiaElEstadoYLiberaElStock() {
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(Pedido.crear(5L, NOW.minusHours(1))));

        Pedido pedido = pedidoService.cancelar(10L);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        verify(inventarioClient).liberarPorPedido(10L);
    }

    @Test
    void cancelarUnPedidoExpiradoFallaYNoLiberaStock() {
        Pedido expirado = Pedido.crear(5L, NOW.minusDays(2));
        expirado.expirar(NOW.minusDays(1));
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> pedidoService.cancelar(10L)).isInstanceOf(TransicionEstadoInvalidaException.class);

        verifyNoInteractions(inventarioClient);
    }
}
