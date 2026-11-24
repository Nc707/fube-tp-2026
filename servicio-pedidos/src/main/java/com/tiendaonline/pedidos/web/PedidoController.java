package com.tiendaonline.pedidos.web;

import com.tiendaonline.pedidos.service.PedidoService;
import com.tiendaonline.pedidos.web.dto.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Consulta y gestion de pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

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
