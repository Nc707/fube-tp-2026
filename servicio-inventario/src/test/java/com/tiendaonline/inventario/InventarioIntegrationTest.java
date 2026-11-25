package com.tiendaonline.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiendaonline.inventario.domain.EstadoReserva;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import com.tiendaonline.inventario.repository.ProductoRepository;
import com.tiendaonline.inventario.repository.ReservaRepository;
import com.tiendaonline.inventario.service.ItemReserva;
import com.tiendaonline.inventario.service.ReservaService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test de integracion de punta a punta: HTTP -> servicio -> JPA -> PostgreSQL real (migrado con Flyway).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ReservaService reservaService;

    private long tecladoId;
    private long mouseId;

    @BeforeEach
    void limpiarYCargarDatos() {
        jdbcTemplate.execute("DELETE FROM reservas");
        jdbcTemplate.execute("DELETE FROM productos");
        jdbcTemplate.execute("ALTER TABLE reservas ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE productos ALTER COLUMN id RESTART WITH 1");
        tecladoId = insertarProducto("TEC-001", "Teclado", "100.00", 10);
        mouseId = insertarProducto("MOU-001", "Mouse", "25.50", 5);
    }

    private long insertarProducto(String sku, String nombre, String precio, int stock) {
        return new org.springframework.jdbc.core.simple.SimpleJdbcInsert(jdbcTemplate)
                .withTableName("productos")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(java.util.Map.of(
                        "sku", sku,
                        "nombre", nombre,
                        "precio", new java.math.BigDecimal(precio),
                        "cantidad_disponible", stock))
                .longValue();
    }

    private int stock(long productoId) {
        return productoRepository.findById(productoId).orElseThrow().getCantidadDisponible();
    }

    private String reservaJson(long pedidoId, long productoId, int cantidad, long productoId2, int cantidad2) {
        return """
                {"pedidoId": %d, "items": [
                  {"productoId": %d, "cantidad": %d},
                  {"productoId": %d, "cantidad": %d}
                ]}
                """.formatted(pedidoId, productoId, cantidad, productoId2, cantidad2);
    }

    @Test
    void consultarDisponibilidad() throws Exception {
        mockMvc.perform(get("/api/v1/productos/{id}/disponibilidad", tecladoId).param("cantidad", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teclado"))
                .andExpect(jsonPath("$.cantidadDisponible").value(10))
                .andExpect(jsonPath("$.suficiente").value(true));
    }

    @Test
    void reservarYLiberarUnaReservaActualizaElStock() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservaJson(1, tecladoId, 3, mouseId, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservas", hasSize(2)))
                .andExpect(jsonPath("$.reservas[1].precioUnitario").value(25.50));

        assertThat(stock(tecladoId)).isEqualTo(7);
        assertThat(stock(mouseId)).isZero();

        Long reservaTecladoId = reservaRepository.findByPedidoIdOrderByIdAsc(1L).get(0).getId();
        mockMvc.perform(post("/api/v1/reservas/{id}/liberacion", reservaTecladoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LIBERADA"))
                .andExpect(jsonPath("$.fechaLiberacion").isNotEmpty());

        assertThat(stock(tecladoId)).isEqualTo(10);
        assertThat(stock(mouseId)).isZero();

        mockMvc.perform(post("/api/v1/reservas/{id}/liberacion", reservaTecladoId))
                .andExpect(status().isConflict());
    }

    @Test
    void reservaSinStockSuficienteNoReservaNingunItem() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservaJson(2, tecladoId, 3, mouseId, 6)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.faltantes", hasSize(1)))
                .andExpect(jsonPath("$.faltantes[0].productoId").value(mouseId));

        assertThat(stock(tecladoId)).isEqualTo(10);
        assertThat(stock(mouseId)).isEqualTo(5);
        assertThat(reservaRepository.count()).isZero();
    }

    @Test
    void reservaConProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservaJson(3, tecladoId, 1, 999, 1)))
                .andExpect(status().isNotFound());

        assertThat(stock(tecladoId)).isEqualTo(10);
    }

    @Test
    void noSePuedeReservarDosVecesElMismoPedido() throws Exception {
        String body = reservaJson(4, tecladoId, 1, mouseId, 1);
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://tiendaonline.com/problemas/pedido-con-reservas-activas"));
    }

    @Test
    void liberarPorPedidoLiberaTodoYEsIdempotente() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservaJson(5, tecladoId, 2, mouseId, 2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reservas/liberacion").param("pedidoId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservas", hasSize(2)))
                .andExpect(jsonPath("$.reservas[0].estado").value("LIBERADA"));

        assertThat(stock(tecladoId)).isEqualTo(10);
        assertThat(stock(mouseId)).isEqualTo(5);

        mockMvc.perform(post("/api/v1/reservas/liberacion").param("pedidoId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservas", hasSize(0)));

        mockMvc.perform(get("/api/v1/reservas").param("pedidoId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservas", hasSize(2)));
    }

    @Test
    void reservasConcurrentesNuncaSobrevendenElStock() throws Exception {
        int hilos = 20;
        AtomicLong pedidoSecuencia = new AtomicLong(100);
        List<Callable<Boolean>> tareas = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            tareas.add(() -> {
                try {
                    reservaService.reservar(pedidoSecuencia.incrementAndGet(), List.of(new ItemReserva(mouseId, 1)));
                    return true;
                } catch (StockInsuficienteException ex) {
                    return false;
                }
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        try {
            long exitosas = 0;
            for (Future<Boolean> resultado : executor.invokeAll(tareas)) {
                if (resultado.get()) {
                    exitosas++;
                }
            }
            assertThat(exitosas).isEqualTo(5);
        } finally {
            executor.shutdown();
        }

        assertThat(stock(mouseId)).isZero();
        assertThat(reservaRepository.findAll())
                .hasSize(5)
                .allSatisfy(r -> assertThat(r.getEstado()).isEqualTo(EstadoReserva.ACTIVA));
    }
}
