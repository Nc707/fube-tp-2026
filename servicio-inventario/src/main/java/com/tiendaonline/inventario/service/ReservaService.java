package com.tiendaonline.inventario.service;

import com.tiendaonline.inventario.domain.EstadoReserva;
import com.tiendaonline.inventario.domain.Producto;
import com.tiendaonline.inventario.domain.Reserva;
import com.tiendaonline.inventario.exception.PedidoConReservasActivasException;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.exception.ReservaNoEncontradaException;
import com.tiendaonline.inventario.exception.SolicitudInvalidaException;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import com.tiendaonline.inventario.repository.ProductoRepository;
import com.tiendaonline.inventario.repository.ReservaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ProductoRepository productoRepository;
    private final Clock clock;

    /**
     * Reserva stock para todos los items de un pedido de forma atomica: o se reservan todos,
     * o no se reserva ninguno (la transaccion hace rollback).
     */
    @Transactional
    public List<Reserva> reservar(Long pedidoId, List<ItemReserva> items) {
        validarSinProductosDuplicados(items);

        if (reservaRepository.existsByPedidoIdAndEstado(pedidoId, EstadoReserva.ACTIVA)) {
            throw new PedidoConReservasActivasException(pedidoId);
        }

        List<Long> productoIds = items.stream().map(ItemReserva::productoId).toList();
        Map<Long, Producto> productos = productoRepository.findAllByIdForUpdate(productoIds).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        for (Long productoId : productoIds) {
            if (!productos.containsKey(productoId)) {
                throw new ProductoNoEncontradoException(productoId);
            }
        }

        List<StockInsuficienteException.Faltante> faltantes = items.stream()
                .filter(item -> !productos.get(item.productoId()).tieneStock(item.cantidad()))
                .map(item -> new StockInsuficienteException.Faltante(item.productoId(), item.cantidad(),
                        productos.get(item.productoId()).getCantidadDisponible()))
                .toList();
        if (!faltantes.isEmpty()) {
            log.warn("Reserva rechazada por stock insuficiente pedidoId={} faltantes={}", pedidoId, faltantes);
            throw new StockInsuficienteException(faltantes);
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Reserva> reservas = items.stream()
                .map(item -> Reserva.crear(productos.get(item.productoId()), pedidoId, item.cantidad(), ahora))
                .toList();
        List<Reserva> guardadas = reservaRepository.saveAll(reservas);
        log.info("Stock reservado pedidoId={} reservaIds={}", pedidoId,
                guardadas.stream().map(Reserva::getId).toList());
        return guardadas;
    }

    @Transactional(readOnly = true)
    public Reserva obtener(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorPedido(Long pedidoId) {
        return reservaRepository.findByPedidoIdOrderByIdAsc(pedidoId);
    }

    /** Libera una reserva puntual devolviendo el stock. Falla si ya estaba liberada. */
    @Transactional
    public Reserva liberar(Long reservaId) {
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));
        bloquearProductos(List.of(reserva));
        reserva.liberar(LocalDateTime.now(clock));
        log.info("Reserva liberada reservaId={} pedidoId={} productoId={} cantidad={}",
                reserva.getId(), reserva.getPedidoId(), reserva.getProducto().getId(), reserva.getCantidad());
        return reserva;
    }

    /**
     * Libera todas las reservas ACTIVAS de un pedido (cancelacion / expiracion del pedido).
     * Es idempotente: si no hay reservas activas devuelve una lista vacia.
     */
    @Transactional
    public List<Reserva> liberarPorPedido(Long pedidoId) {
        List<Reserva> activas = reservaRepository.findByPedidoIdAndEstadoForUpdate(pedidoId, EstadoReserva.ACTIVA);
        bloquearProductos(activas);
        LocalDateTime ahora = LocalDateTime.now(clock);
        activas.forEach(reserva -> reserva.liberar(ahora));
        log.info("Reservas del pedido liberadas pedidoId={} cantidadReservas={}", pedidoId, activas.size());
        return activas;
    }

    /** Toma el bloqueo de los productos afectados antes de devolverles stock. */
    private void bloquearProductos(List<Reserva> reservas) {
        if (reservas.isEmpty()) {
            return;
        }
        Set<Long> ids = reservas.stream().map(r -> r.getProducto().getId()).collect(Collectors.toSet());
        productoRepository.findAllByIdForUpdate(ids);
    }

    private static void validarSinProductosDuplicados(List<ItemReserva> items) {
        Set<Long> vistos = new HashSet<>();
        for (ItemReserva item : items) {
            if (!vistos.add(item.productoId())) {
                throw new SolicitudInvalidaException(
                        "El producto " + item.productoId() + " aparece mas de una vez en la solicitud");
            }
        }
    }
}
