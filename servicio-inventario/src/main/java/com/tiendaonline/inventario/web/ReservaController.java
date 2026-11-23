package com.tiendaonline.inventario.web;

import com.tiendaonline.inventario.domain.Reserva;
import com.tiendaonline.inventario.service.ItemReserva;
import com.tiendaonline.inventario.service.ReservaService;
import com.tiendaonline.inventario.web.dto.CrearReservaRequest;
import com.tiendaonline.inventario.web.dto.ReservaResponse;
import com.tiendaonline.inventario.web.dto.ReservasPedidoResponse;
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
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Reserva y liberacion de stock para pedidos")
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @Operation(summary = "Reservar stock para un pedido",
            description = "Reserva todos los items del pedido de forma atomica. Si algun producto no tiene "
                    + "stock suficiente, no se reserva ninguno y se responde 409 con el detalle de faltantes.")
    @ApiResponse(responseCode = "201", description = "Reservas creadas")
    @ApiResponse(responseCode = "400", description = "Solicitud invalida",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Algun producto no existe",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente o el pedido ya tiene reservas activas",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ReservasPedidoResponse> reservar(@Valid @RequestBody CrearReservaRequest request) {
        List<ItemReserva> items = request.items().stream()
                .map(item -> new ItemReserva(item.productoId(), item.cantidad()))
                .toList();
        List<Reserva> reservas = reservaService.reservar(request.pedidoId(), items);
        URI location = URI.create("/api/v1/reservas?pedidoId=" + request.pedidoId());
        return ResponseEntity.created(location).body(ReservasPedidoResponse.de(request.pedidoId(), reservas));
    }

    @GetMapping("/{reservaId}")
    @Operation(summary = "Consultar una reserva por id")
    @ApiResponse(responseCode = "200", description = "Reserva encontrada")
    @ApiResponse(responseCode = "404", description = "La reserva no existe",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ReservaResponse obtener(@PathVariable @Positive Long reservaId) {
        return ReservaResponse.de(reservaService.obtener(reservaId));
    }

    @GetMapping
    @Operation(summary = "Listar las reservas de un pedido")
    public ReservasPedidoResponse listarPorPedido(@RequestParam @Positive Long pedidoId) {
        return ReservasPedidoResponse.de(pedidoId, reservaService.listarPorPedido(pedidoId));
    }

    @PostMapping("/{reservaId}/liberacion")
    @Operation(summary = "Liberar una reserva existente",
            description = "Pasa la reserva a LIBERADA y devuelve las unidades al stock disponible.")
    @ApiResponse(responseCode = "200", description = "Reserva liberada")
    @ApiResponse(responseCode = "404", description = "La reserva no existe",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "La reserva ya estaba liberada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ReservaResponse liberar(@PathVariable @Positive Long reservaId) {
        return ReservaResponse.de(reservaService.liberar(reservaId));
    }

    @PostMapping("/liberacion")
    @Operation(summary = "Liberar todas las reservas activas de un pedido",
            description = "Pensado para la cancelacion y la expiracion de pedidos. Es idempotente: si el pedido "
                    + "no tiene reservas activas devuelve una lista vacia.")
    @ApiResponse(responseCode = "200", description = "Reservas liberadas (puede ser una lista vacia)")
    public ReservasPedidoResponse liberarPorPedido(@RequestParam @Positive Long pedidoId) {
        return ReservasPedidoResponse.de(pedidoId, reservaService.liberarPorPedido(pedidoId));
    }
}
