package com.tiendaonline.pedidos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    @Mock
    private Clock clock;

    @InjectMocks
    private PedidoService pedidoService;

    // el reloj mockeado devuelve siempre NOW, para poder verificar las fechas
    private void relojEnNow() {
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.instant()).thenReturn(NOW.atZone(ZoneId.systemDefault()).toInstant());
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

        assertSame(pedido, pedidoService.obtener(1L));
    }

    @Test
    void obtenerPedidoInexistenteLanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        PedidoNoEncontradoException ex = assertThrows(PedidoNoEncontradoException.class,
                () -> pedidoService.obtener(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void listarPorClienteDevuelveLosPedidosDelCliente() {
        List<Pedido> pedidos = List.of(Pedido.crear(5L, NOW), Pedido.crear(5L, NOW.plusHours(1)));
        when(pedidoRepository.findByClienteIdOrderByIdAsc(5L)).thenReturn(pedidos);

        assertEquals(pedidos, pedidoService.listarPorCliente(5L));
    }

    @Test
    void listarPorClienteSinPedidosDevuelveListaVacia() {
        when(pedidoRepository.findByClienteIdOrderByIdAsc(7L)).thenReturn(List.of());

        assertTrue(pedidoService.listarPorCliente(7L).isEmpty());
    }

    @Test
    void crearReservaElStockYCalculaElTotalConLosPreciosDeInventario() {
        relojEnNow();
        guardarAsignaId();
        when(inventarioClient.reservar(10L, ITEMS)).thenReturn(List.of(
                new ReservaInventario(1L, 1L, 2, new BigDecimal("45000.00")),
                new ReservaInventario(2L, 2L, 1, new BigDecimal("18500.00"))));

        Pedido pedido = pedidoService.crear(5L, ITEMS);

        verify(clientesClient).verificarExistencia(5L);
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        assertEquals(2, pedido.getItems().size());
        assertEquals(new BigDecimal("108500.00"), pedido.getMontoTotal());
        assertEquals(NOW, pedido.getFechaCreacion());
    }

    @Test
    void crearConClienteInexistenteNoGuardaNiReserva() {
        doThrow(new ClienteNoEncontradoException(5L)).when(clientesClient).verificarExistencia(5L);

        assertThrows(ClienteNoEncontradoException.class, () -> pedidoService.crear(5L, ITEMS));

        verify(pedidoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(inventarioClient);
    }

    @Test
    void crearSinStockFallaYNoLiberaPorqueInventarioNoReservoNada() {
        relojEnNow();
        guardarAsignaId();
        when(inventarioClient.reservar(anyLong(), anyList()))
                .thenThrow(new StockInsuficienteException(List.of(new StockInsuficienteException.Faltante(1L, 2, 0))));

        assertThrows(StockInsuficienteException.class, () -> pedidoService.crear(5L, ITEMS));

        verify(inventarioClient, never()).liberarPorPedido(anyLong());
    }

    @Test
    void crearConInventarioNoDisponibleLiberaPorLasDudas() {
        relojEnNow();
        guardarAsignaId();
        when(inventarioClient.reservar(anyLong(), anyList()))
                .thenThrow(new ServicioNoDisponibleException("servicio-inventario", "timeout", null));

        assertThrows(ServicioNoDisponibleException.class, () -> pedidoService.crear(5L, ITEMS));

        verify(inventarioClient).liberarPorPedido(10L);
    }

    @Test
    void cancelarCambiaElEstadoYLiberaElStock() {
        relojEnNow();
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(Pedido.crear(5L, NOW.minusHours(1))));

        Pedido pedido = pedidoService.cancelar(10L);

        assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
        verify(inventarioClient).liberarPorPedido(10L);
    }

    @Test
    void cancelarUnPedidoExpiradoFallaYNoLiberaStock() {
        relojEnNow();
        Pedido expirado = Pedido.crear(5L, NOW.minusDays(2));
        expirado.expirar(NOW.minusDays(1));
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(expirado));

        assertThrows(TransicionEstadoInvalidaException.class, () -> pedidoService.cancelar(10L));

        verifyNoInteractions(inventarioClient);
    }
}
