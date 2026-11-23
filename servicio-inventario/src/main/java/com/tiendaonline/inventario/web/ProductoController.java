package com.tiendaonline.inventario.web;

import com.tiendaonline.inventario.service.ProductoService;
import com.tiendaonline.inventario.web.dto.DisponibilidadResponse;
import com.tiendaonline.inventario.web.dto.ProductoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catalogo y disponibilidad de stock")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @Operation(summary = "Listar productos")
    public List<ProductoResponse> listar() {
        return productoService.listar().stream().map(ProductoResponse::de).toList();
    }

    @GetMapping("/{productoId}")
    @Operation(summary = "Consultar un producto por id")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "El producto no existe",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ProductoResponse obtener(@PathVariable @Positive Long productoId) {
        return ProductoResponse.de(productoService.obtener(productoId));
    }

    @GetMapping("/{productoId}/disponibilidad")
    @Operation(summary = "Consultar la disponibilidad de stock de un producto",
            description = "Devuelve la cantidad disponible y el precio. Si se envia 'cantidad', indica ademas "
                    + "si el stock alcanza para esa cantidad.")
    @ApiResponse(responseCode = "200", description = "Disponibilidad del producto")
    @ApiResponse(responseCode = "400", description = "Parametros invalidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "El producto no existe",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public DisponibilidadResponse disponibilidad(
            @PathVariable @Positive Long productoId,
            @Parameter(description = "Cantidad a verificar (opcional)")
            @RequestParam(required = false) @Positive Integer cantidad) {
        return DisponibilidadResponse.de(productoService.obtener(productoId), cantidad);
    }
}
