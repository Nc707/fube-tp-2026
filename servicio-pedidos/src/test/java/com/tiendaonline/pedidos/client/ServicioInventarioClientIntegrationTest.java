package com.tiendaonline.pedidos.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import com.tiendaonline.pedidos.exception.ProductoNoEncontradoException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import com.tiendaonline.pedidos.exception.StockInsuficienteException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

//servicio-inventario simulado con MockServer, con las respuestas reales de su API
@SpringBootTest
class ServicioInventarioClientIntegrationTest {

    private static final ClientAndServer inventarioMock = ClientAndServer.startClientAndServer();
    private static final MediaType PROBLEM_JSON = MediaType.parse("application/problem+json");

    @DynamicPropertySource
    static void apuntarAMockServer(DynamicPropertyRegistry registry) {
        registry.add("servicios.inventario.url", () -> "http://localhost:" + inventarioMock.getPort());
        registry.add("servicios.http.read-timeout", () -> "500ms");
    }

    @Autowired
    private ServicioInventarioClient inventarioClient;

    @AfterEach
    void limpiar() {
        inventarioMock.reset();
    }

    @AfterAll
    static void detener() {
        inventarioMock.stop();
    }

    @Test
    void reservarEnviaLosItemsYDevuelveLasReservasConSuPrecio() {
        inventarioMock.when(request().withMethod("POST").withPath("/api/v1/reservas"))
                .respond(response().withStatusCode(201).withContentType(MediaType.APPLICATION_JSON).withBody("""
                        {"pedidoId": 50, "reservas": [
                          {"id": 1, "pedidoId": 50, "productoId": 1, "cantidad": 2, "precioUnitario": 45000.00,
                           "estado": "ACTIVA", "fechaCreacion": "2026-11-24T10:00:00", "fechaLiberacion": null},
                          {"id": 2, "pedidoId": 50, "productoId": 2, "cantidad": 1, "precioUnitario": 18500.00,
                           "estado": "ACTIVA", "fechaCreacion": "2026-11-24T10:00:00", "fechaLiberacion": null}
                        ]}"""));

        List<ReservaInventario> reservas = inventarioClient.reservar(50L,
                List.of(new ItemAReservar(1L, 2), new ItemAReservar(2L, 1)));

        assertThat(reservas).hasSize(2);
        assertThat(reservas.get(0).productoId()).isEqualTo(1L);
        assertThat(reservas.get(0).cantidad()).isEqualTo(2);
        assertThat(reservas.get(0).precioUnitario()).isEqualByComparingTo("45000.00");
        assertThat(reservas.get(1).precioUnitario()).isEqualByComparingTo("18500.00");
        inventarioMock.verify(request().withMethod("POST").withPath("/api/v1/reservas")
                        .withBody(json("""
                                {"pedidoId": 50, "items": [{"productoId": 1, "cantidad": 2},
                                                           {"productoId": 2, "cantidad": 1}]}""",
                                MatchType.STRICT)),
                VerificationTimes.once());
    }

    @Test
    void reservarSinStockLanzaStockInsuficienteConLosFaltantes() {
        inventarioMock.when(request().withMethod("POST").withPath("/api/v1/reservas"))
                .respond(response().withStatusCode(409).withContentType(PROBLEM_JSON).withBody("""
                        {"type": "https://tiendaonline.com/problemas/stock-insuficiente",
                         "title": "Stock insuficiente", "status": 409,
                         "detail": "Stock insuficiente para 1 producto(s)",
                         "faltantes": [{"productoId": 5, "cantidadSolicitada": 3, "cantidadDisponible": 0}]}"""));

        assertThatThrownBy(() -> inventarioClient.reservar(50L, List.of(new ItemAReservar(5L, 3))))
                .isInstanceOfSatisfying(StockInsuficienteException.class, ex ->
                        assertThat(ex.getFaltantes()).containsExactly(
                                new StockInsuficienteException.Faltante(5L, 3, 0)));
    }

    @Test
    void reservarProductoInexistenteLanzaProductoNoEncontrado() {
        inventarioMock.when(request().withMethod("POST").withPath("/api/v1/reservas"))
                .respond(response().withStatusCode(404).withContentType(PROBLEM_JSON).withBody("""
                        {"type": "https://tiendaonline.com/problemas/recurso-no-encontrado",
                         "status": 404, "detail": "No existe el producto con id 999"}"""));

        assertThatThrownBy(() -> inventarioClient.reservar(50L, List.of(new ItemAReservar(999L, 1))))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage("No existe el producto con id 999");
    }

    @Test
    void reservarConConflictoQueNoEsDeStockLanzaServicioNoDisponible() {
        inventarioMock.when(request().withMethod("POST").withPath("/api/v1/reservas"))
                .respond(response().withStatusCode(409).withContentType(PROBLEM_JSON).withBody("""
                        {"type": "https://tiendaonline.com/problemas/pedido-con-reservas-activas",
                         "status": 409, "detail": "El pedido 50 ya tiene reservas activas"}"""));

        assertThatThrownBy(() -> inventarioClient.reservar(50L, List.of(new ItemAReservar(1L, 1))))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void reservarConErrorDelServidorLanzaServicioNoDisponible() {
        inventarioMock.when(request().withPath("/api/v1/reservas")).respond(response().withStatusCode(500));

        assertThatThrownBy(() -> inventarioClient.reservar(50L, List.of(new ItemAReservar(1L, 1))))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("servicio-inventario");
    }

    @Test
    void reservarConTimeoutLanzaServicioNoDisponible() {
        inventarioMock.when(request().withPath("/api/v1/reservas"))
                .respond(response().withStatusCode(201).withDelay(TimeUnit.SECONDS, 2));

        assertThatThrownBy(() -> inventarioClient.reservar(50L, List.of(new ItemAReservar(1L, 1))))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void liberarPorPedidoLlamaAlEndpointDeLiberacion() {
        inventarioMock.when(request().withMethod("POST").withPath("/api/v1/reservas/liberacion"))
                .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"pedidoId\": 50, \"reservas\": []}"));

        assertThatCode(() -> inventarioClient.liberarPorPedido(50L)).doesNotThrowAnyException();

        inventarioMock.verify(request().withMethod("POST").withPath("/api/v1/reservas/liberacion")
                .withQueryStringParameter("pedidoId", "50"), VerificationTimes.once());
    }

    @Test
    void liberarConErrorLanzaServicioNoDisponible() {
        inventarioMock.when(request().withPath("/api/v1/reservas/liberacion")).respond(response().withStatusCode(503));

        assertThatThrownBy(() -> inventarioClient.liberarPorPedido(50L))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
