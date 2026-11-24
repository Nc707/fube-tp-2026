package com.tiendaonline.pedidos.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiendaonline.pedidos.domain.Pedido;
import com.tiendaonline.pedidos.exception.PedidoNoEncontradoException;
import com.tiendaonline.pedidos.service.PedidoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PedidoController.class)
class PedidoControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService pedidoService;

    private Pedido pedidoConItems(Long id) {
        Pedido pedido = Pedido.crear(5L, NOW);
        ReflectionTestUtils.setField(pedido, "id", id);
        pedido.agregarItem(1L, 2, new BigDecimal("45000.00"));
        pedido.agregarItem(2L, 1, new BigDecimal("18500.00"));
        return pedido;
    }

    @Test
    void obtenerDevuelveElPedidoConSusItems() throws Exception {
        when(pedidoService.obtener(10L)).thenReturn(pedidoConItems(10L));

        mockMvc.perform(get("/api/v1/pedidos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.clienteId").value(5))
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.montoTotal").value(108500.00))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productoId").value(1))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.items[0].precioUnitario").value(45000.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(90000.00));
    }

    @Test
    void obtenerPedidoInexistenteDevuelve404() throws Exception {
        when(pedidoService.obtener(99L)).thenThrow(new PedidoNoEncontradoException(99L));

        mockMvc.perform(get("/api/v1/pedidos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("https://tiendaonline.com/problemas/recurso-no-encontrado"));
    }

    @Test
    void obtenerConIdNoPositivoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("pedidoId"));

        verifyNoInteractions(pedidoService);
    }

    @Test
    void obtenerConIdNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pedidoService);
    }

    @Test
    void listarPorClienteDevuelveSusPedidos() throws Exception {
        when(pedidoService.listarPorCliente(5L)).thenReturn(List.of(pedidoConItems(10L), pedidoConItems(11L)));

        mockMvc.perform(get("/api/v1/pedidos").param("clienteId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[0].clienteId").value(5))
                .andExpect(jsonPath("$[0].items", hasSize(2)));
    }

    @Test
    void listarPorClienteSinPedidosDevuelveListaVacia() throws Exception {
        when(pedidoService.listarPorCliente(7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pedidos").param("clienteId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listarSinClienteIdDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pedidoService);
    }

    @Test
    void listarConClienteIdNoPositivoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos").param("clienteId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("clienteId"));

        verifyNoInteractions(pedidoService);
    }
}
