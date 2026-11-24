package com.tiendaonline.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tiendaonline.pedidos.domain.Pedido;
import com.tiendaonline.pedidos.exception.PedidoNoEncontradoException;
import com.tiendaonline.pedidos.repository.PedidoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 10, 0);

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PedidoService pedidoService;

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
}
