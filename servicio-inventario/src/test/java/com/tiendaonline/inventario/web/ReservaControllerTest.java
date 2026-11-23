package com.tiendaonline.inventario.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiendaonline.inventario.domain.Producto;
import com.tiendaonline.inventario.domain.Reserva;
import com.tiendaonline.inventario.exception.ReservaNoEncontradaException;
import com.tiendaonline.inventario.exception.ReservaYaLiberadaException;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import com.tiendaonline.inventario.service.ItemReserva;
import com.tiendaonline.inventario.service.ReservaService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservaController.class)
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;

    private final Producto teclado = new Producto(1L, "TEC-001", "Teclado", new BigDecimal("100.00"), 10);

    @Test
    void reservarDevuelve201ConLasReservas() throws Exception {
        when(reservaService.reservar(eq(50L), eq(List.of(new ItemReserva(1L, 2)))))
                .thenReturn(List.of(Reserva.crear(teclado, 50L, 2, LocalDateTime.now())));

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId": 50, "items": [{"productoId": 1, "cantidad": 2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/reservas?pedidoId=50"))
                .andExpect(jsonPath("$.pedidoId").value(50))
                .andExpect(jsonPath("$.reservas", hasSize(1)))
                .andExpect(jsonPath("$.reservas[0].productoId").value(1))
                .andExpect(jsonPath("$.reservas[0].precioUnitario").value(100.00))
                .andExpect(jsonPath("$.reservas[0].estado").value("ACTIVA"));
    }

    @Test
    void reservarConBodyInvalidoDevuelve400ConDetalleDeErrores() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [{"productoId": 1, "cantidad": 0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Datos invalidos"))
                .andExpect(jsonPath("$.errores", hasSize(2)));
        verifyNoInteractions(reservaService);
    }

    @Test
    void reservarSinItemsDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId": 50, "items": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("items"));
    }

    @Test
    void reservarSinStockDevuelve409ConFaltantes() throws Exception {
        when(reservaService.reservar(eq(50L), any())).thenThrow(new StockInsuficienteException(
                List.of(new StockInsuficienteException.Faltante(1L, 20, 10))));

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId": 50, "items": [{"productoId": 1, "cantidad": 20}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://tiendaonline.com/problemas/stock-insuficiente"))
                .andExpect(jsonPath("$.faltantes[0].productoId").value(1))
                .andExpect(jsonPath("$.faltantes[0].cantidadSolicitada").value(20))
                .andExpect(jsonPath("$.faltantes[0].cantidadDisponible").value(10));
    }

    @Test
    void obtenerReservaInexistenteDevuelve404() throws Exception {
        when(reservaService.obtener(7L)).thenThrow(new ReservaNoEncontradaException(7L));

        mockMvc.perform(get("/api/v1/reservas/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No existe la reserva con id 7"));
    }

    @Test
    void liberarReservaYaLiberadaDevuelve409() throws Exception {
        when(reservaService.liberar(7L)).thenThrow(new ReservaYaLiberadaException(7L));

        mockMvc.perform(post("/api/v1/reservas/7/liberacion"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Reserva ya liberada"));
    }

    @Test
    void idConFormatoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/reservas/abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reservaService);
    }

    @Test
    void idNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/reservas/-1/liberacion"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("reservaId"));
        verifyNoInteractions(reservaService);
    }
}
