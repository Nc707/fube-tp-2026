package com.tiendaonline.pedidos.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.tiendaonline.pedidos.domain.EstadoPedido;
import com.tiendaonline.pedidos.domain.Pedido;
import com.tiendaonline.pedidos.repository.PedidoRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Corre el job de expiracion contra la base de test, con servicio-inventario simulado con MockServer
@SpringBootTest
@ActiveProfiles("test")
class ExpiracionPedidosJobIntegrationTest {

    static final ClientAndServer inventario = ClientAndServer.startClientAndServer();

    @DynamicPropertySource
    static void apuntarAMockServer(DynamicPropertyRegistry registry) {
        registry.add("servicios.inventario.url", () -> "http://localhost:" + inventario.getPort());
    }

    @Autowired
    ExpiracionPedidosScheduler scheduler;
    @Autowired
    PedidoRepository pedidoRepository;

    @BeforeEach
    void limpiar() {
        inventario.reset();
        pedidoRepository.deleteAll();
    }

    @AfterAll
    static void detener() {
        inventario.stop();
    }

    @Test
    void expiraSoloLosPedidosCreadosConMasDe24hsYLiberaSuStock() throws Exception {
        LocalDateTime ahora = LocalDateTime.now();
        Pedido vencido = pedidoRepository.save(Pedido.crear(1L, ahora.minusHours(25)));
        Pedido reciente = pedidoRepository.save(Pedido.crear(1L, ahora.minusHours(1)));
        Pedido confirmado = Pedido.crear(1L, ahora.minusHours(30));
        confirmado.confirmar(ahora.minusHours(29));
        confirmado = pedidoRepository.save(confirmado);
        inventario.when(request().withMethod("POST").withPath("/api/v1/reservas/liberacion"))
                .respond(response().withStatusCode(200));

        ResumenExpiracion resumen = scheduler.ejecutar();

        assertThat(resumen.estado()).isEqualTo("COMPLETED");
        assertThat(resumen.leidos()).isEqualTo(1);
        assertThat(resumen.expirados()).isEqualTo(1);
        assertThat(estadoDe(vencido)).isEqualTo(EstadoPedido.EXPIRADO);
        assertThat(estadoDe(reciente)).isEqualTo(EstadoPedido.CREADO);
        assertThat(estadoDe(confirmado)).isEqualTo(EstadoPedido.CONFIRMADO);
        inventario.verify(request().withPath("/api/v1/reservas/liberacion")
                .withQueryStringParameter("pedidoId", vencido.getId().toString()), VerificationTimes.once());
        inventario.verify(request().withPath("/api/v1/reservas/liberacion"), VerificationTimes.once());
    }

    private EstadoPedido estadoDe(Pedido pedido) {
        return pedidoRepository.findById(pedido.getId()).orElseThrow().getEstado();
    }
}
