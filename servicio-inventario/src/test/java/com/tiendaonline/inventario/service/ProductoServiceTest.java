package com.tiendaonline.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tiendaonline.inventario.domain.Producto;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    @Test
    void obtenerDevuelveElProducto() {
        Producto producto = new Producto(1L, "TEC-001", "Teclado", new BigDecimal("100.00"), 5);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThat(productoService.obtener(1L)).isSameAs(producto);
    }

    @Test
    void obtenerProductoInexistenteLanzaExcepcion() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtener(99L))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
