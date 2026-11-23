package com.tiendaonline.inventario.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiendaonline.inventario.domain.Producto;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.service.ProductoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductoService productoService;

    @BeforeEach
    void setUp() {
        when(productoService.obtener(1L))
                .thenReturn(new Producto(1L, "TEC-001", "Teclado", new BigDecimal("100.00"), 10));
    }

    @Test
    void disponibilidadDevuelveStockYPrecio() throws Exception {
        mockMvc.perform(get("/api/v1/productos/1/disponibilidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.cantidadDisponible").value(10))
                .andExpect(jsonPath("$.precio").value(100.00))
                .andExpect(jsonPath("$.suficiente").doesNotExist());
    }

    @Test
    void disponibilidadConCantidadIndicaSiAlcanza() throws Exception {
        mockMvc.perform(get("/api/v1/productos/1/disponibilidad").param("cantidad", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadSolicitada").value(10))
                .andExpect(jsonPath("$.suficiente").value(true));

        mockMvc.perform(get("/api/v1/productos/1/disponibilidad").param("cantidad", "11"))
                .andExpect(jsonPath("$.suficiente").value(false));
    }

    @Test
    void disponibilidadConCantidadInvalidaDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/productos/1/disponibilidad").param("cantidad", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("cantidad"));
    }

    @Test
    void disponibilidadDeProductoInexistenteDevuelve404() throws Exception {
        when(productoService.obtener(99L)).thenThrow(new ProductoNoEncontradoException(99L));

        mockMvc.perform(get("/api/v1/productos/99/disponibilidad"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://tiendaonline.com/problemas/recurso-no-encontrado"));
    }
}
