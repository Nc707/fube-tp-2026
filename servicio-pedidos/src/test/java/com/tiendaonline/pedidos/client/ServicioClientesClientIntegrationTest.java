package com.tiendaonline.pedidos.client;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// servicio-clientes simulado con MockServer
@SpringBootTest
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
        clientesMock.when(request().withMethod("GET").withPath("/clients/5"))
                .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"id\":5,\"name\":\"J***\",\"surname\":\"P***\",\"email\":\"****@****.com\"}"));

        assertThatCode(() -> clientesClient.verificarExistencia(5L)).doesNotThrowAnyException();

        clientesMock.verify(request().withMethod("GET").withPath("/clients/5"), VerificationTimes.once());
    }

    @Test
    void clienteInexistenteLanzaClienteNoEncontrado() {
        clientesMock.when(request().withMethod("GET").withPath("/clients/99"))
                .respond(response().withStatusCode(404)
                        .withContentType(MediaType.parse("application/problem+json"))
                        .withBody("{\"status\":404,\"detail\":\"Client not found\"}"));

        assertThatThrownBy(() -> clientesClient.verificarExistencia(99L))
                .isInstanceOf(ClienteNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void errorDelServicioLanzaServicioNoDisponible() {
        clientesMock.when(request().withPath("/clients/5")).respond(response().withStatusCode(500));

        assertThatThrownBy(() -> clientesClient.verificarExistencia(5L))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("servicio-clientes");
    }

    @Test
    void timeoutLanzaServicioNoDisponible() {
        clientesMock.when(request().withPath("/clients/5"))
                .respond(response().withStatusCode(200).withDelay(TimeUnit.SECONDS, 2));

        assertThatThrownBy(() -> clientesClient.verificarExistencia(5L))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
