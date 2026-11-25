package com.tiendaonline.pedidos.web;

import com.tiendaonline.pedidos.client.ItemAReservar;
import com.tiendaonline.pedidos.domain.Pedido;
import com.tiendaonline.pedidos.service.PedidoService;
import com.tiendaonline.pedidos.web.dto.CrearPedidoRequest;
import com.tiendaonline.pedidos.web.dto.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Consulta y gestion de pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @Operation(summary = "Crear un pedido",
            description = "Verifica el cliente en servicio-clientes, reserva el stock en servicio-inventario y "
                    + "calcula el monto total. Si algo falla, el pedido no se crea.")
    @ApiResponse(responseCode = "201", description = "Pedido creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "El cliente o algun producto no existe",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "servicio-clientes o servicio-inventario no disponible",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest request) {
        List<ItemAReservar> items = request.items().stream()
                .map(item -> new ItemAReservar(item.productoId(), item.cantidad()))
                .toList();
        Pedido pedido = pedidoService.crear(request.clienteId(), items);
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + pedido.getId()))
                .body(PedidoResponse.de(pedido));
    }

    @PostMapping("/{pedidoId}/cancelacion")
    @Operation(summary = "Cancelar un pedido",
            description = "Solo si esta CREADO o CONFIRMADO. Libera el stock reservado en servicio-inventario.")
    @ApiResponse(responseCode = "200", description = "Pedido cancelado")
    @ApiResponse(responseCode = "404", description = "El pedido no existe",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "El pedido no se puede cancelar en su estado actual",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PedidoResponse cancelar(@PathVariable @Positive Long pedidoId) {
        return PedidoResponse.de(pedidoService.cancelar(pedidoId));
    }

    @GetMapping("/{pedidoId}")
    @Operation(summary = "Consultar un pedido por id")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "400", description = "Id invalido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "El pedido no existe",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PedidoResponse obtener(@PathVariable @Positive Long pedidoId) {
        return PedidoResponse.de(pedidoService.obtener(pedidoId));
    }

    @GetMapping
    @Operation(summary = "Listar los pedidos de un cliente",
            description = "Lo consume servicio-clientes. Devuelve una lista vacia si el cliente no tiene pedidos.")
    @ApiResponse(responseCode = "200", description = "Pedidos del cliente (puede ser una lista vacia)")
    @ApiResponse(responseCode = "400", description = "Falta clienteId o es invalido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public List<PedidoResponse> listarPorCliente(@RequestParam @Positive Long clienteId) {
        return pedidoService.listarPorCliente(clienteId).stream().map(PedidoResponse::de).toList();
    }
}
