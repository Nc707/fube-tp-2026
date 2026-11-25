package com.tiendaonline.pedidos.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.tiendaonline.pedidos.exception.ClienteNoEncontradoException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// servicio-clientes simulado con MockServer
@SpringBootTest
@ActiveProfiles("test")
class ServicioClientesClientIntegrationTest {

    private static final ClientAndServer clientesMock = ClientAndServer.startClientAndServer();

    @DynamicPropertySource
    static void apuntarAMockServer(DynamicPropertyRegistry registry) {
        registry.add("servicios.clientes.url", () -> "http://localhost:" + clientesMock.getPort());
        registry.add("servicios.http.read-timeout", () -> "500ms");
    }

    @Autowired
    private ServicioClientesClient clientesClient;

    @AfterEach
    void limpiar() {
        clientesMock.reset();
    }

    @AfterAll
    static void detener() {
        clientesMock.stop();
    }

    @Test
    void clienteExistenteNoLanzaExcepcion() {
        clientesMock.when(request().withMethod("GET").withPath("/api/v1/clientes/5"))
                .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"id\":5,\"name\":\"J***\",\"surname\":\"P***\",\"email\":\"****@****.com\"}"));

        assertDoesNotThrow(() -> clientesClient.verificarExistencia(5L));

        clientesMock.verify(request().withMethod("GET").withPath("/api/v1/clientes/5"), VerificationTimes.once());
    }

    @Test
    void clienteInexistenteLanzaClienteNoEncontrado() {
        clientesMock.when(request().withMethod("GET").withPath("/api/v1/clientes/99"))
                .respond(response().withStatusCode(404)
                        .withContentType(MediaType.parse("application/problem+json"))
                        .withBody("{\"status\":404,\"detail\":\"Client not found\"}"));

        ClienteNoEncontradoException ex = assertThrows(ClienteNoEncontradoException.class,
                () -> clientesClient.verificarExistencia(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void errorDelServicioLanzaServicioNoDisponible() {
        clientesMock.when(request().withPath("/api/v1/clientes/5")).respond(response().withStatusCode(500));

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
                () -> clientesClient.verificarExistencia(5L));
        assertTrue(ex.getMessage().contains("servicio-clientes"));
    }

    @Test
    void timeoutLanzaServicioNoDisponible() {
        clientesMock.when(request().withPath("/api/v1/clientes/5"))
                .respond(response().withStatusCode(200).withDelay(TimeUnit.SECONDS, 2));

        assertThrows(ServicioNoDisponibleException.class, () -> clientesClient.verificarExistencia(5L));
    }
}
