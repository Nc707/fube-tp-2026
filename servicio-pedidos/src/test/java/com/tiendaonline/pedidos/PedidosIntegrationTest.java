package com.tiendaonline.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiendaonline.pedidos.domain.EstadoPedido;
import com.tiendaonline.pedidos.domain.Pedido;
import com.tiendaonline.pedidos.repository.PedidoRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

//Crear y cancelar pedidos , con servicio-clientes y servicio-inventario simulados con MockServer
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidosIntegrationTest {

    static final ClientAndServer otrosServicios = ClientAndServer.startClientAndServer();

    @DynamicPropertySource
    static void apuntarAMockServer(DynamicPropertyRegistry registry) {
        registry.add("servicios.clientes.url", () -> "http://localhost:" + otrosServicios.getPort());
        registry.add("servicios.inventario.url", () -> "http://localhost:" + otrosServicios.getPort());
    }

    private static final String PEDIDO = """
            {"clienteId": 1, "items": [{"productoId": 1, "cantidad": 2}, {"productoId": 2, "cantidad": 1}]}""";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    PedidoRepository pedidoRepository;

    @BeforeEach
    void limpiar() {
        otrosServicios.reset();
        pedidoRepository.deleteAll();
    }

    @AfterAll
    static void detener() {
        otrosServicios.stop();
    }

    private void clienteExiste() {
        otrosServicios.when(request().withMethod("GET").withPath("/api/v1/clientes/1"))
                .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"id\": 1}"));
    }

    @Test
    void crearPedidoReservaElStockYCalculaElTotal() throws Exception {
        clienteExiste();
        otrosServicios.when(request().withMethod("POST").withPath("/api/v1/reservas"))
                .respond(response().withStatusCode(201).withContentType(MediaType.APPLICATION_JSON).withBody("""
                        {"pedidoId": 1, "reservas": [
                          {"id": 1, "productoId": 1, "cantidad": 2, "precioUnitario": 45000.00},
                          {"id": 2, "productoId": 2, "cantidad": 1, "precioUnitario": 18500.00}]}"""));

        mockMvc.perform(post("/api/v1/pedidos").contentType("application/json").content(PEDIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.montoTotal").value(108500.00))
                .andExpect(jsonPath("$.items.length()").value(2));

        assertThat(pedidoRepository.count()).isEqualTo(1);
    }

    @Test
    void sinStockElPedidoNoSeCrea() throws Exception {
        clienteExiste();
        otrosServicios.when(request().withMethod("POST").withPath("/api/v1/reservas"))
                .respond(response().withStatusCode(409).withContentType(MediaType.parse("application/problem+json"))
                        .withBody("""
                                {"type": "https://tiendaonline.com/problemas/stock-insuficiente", "status": 409,
                                 "faltantes": [{"productoId": 1, "cantidadSolicitada": 2, "cantidadDisponible": 0}]}"""));

        mockMvc.perform(post("/api/v1/pedidos").contentType("application/json").content(PEDIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.faltantes[0].productoId").value(1));

        assertThat(pedidoRepository.count()).isZero();
    }

    @Test
    void conClienteInexistenteElPedidoNoSeCreaYNoSeReservaStock() throws Exception {
        otrosServicios.when(request().withMethod("GET").withPath("/api/v1/clientes/1"))
                .respond(response().withStatusCode(404));

        mockMvc.perform(post("/api/v1/pedidos").contentType("application/json").content(PEDIDO))
                .andExpect(status().isNotFound());

        assertThat(pedidoRepository.count()).isZero();
        otrosServicios.verify(request().withPath("/api/v1/reservas"), VerificationTimes.never());
    }

    @Test
    void cancelarPedidoLiberaElStock() throws Exception {
        Pedido pedido = pedidoRepository.save(Pedido.crear(1L, LocalDateTime.now()));
        otrosServicios.when(request().withMethod("POST").withPath("/api/v1/reservas/liberacion"))
                .respond(response().withStatusCode(200));

        mockMvc.perform(post("/api/v1/pedidos/" + pedido.getId() + "/cancelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        assertThat(pedidoRepository.findById(pedido.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CANCELADO);
        otrosServicios.verify(request().withPath("/api/v1/reservas/liberacion")
                .withQueryStringParameter("pedidoId", pedido.getId().toString()), VerificationTimes.once());
    }
}
