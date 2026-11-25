package com.tiendaonline.pedidos.service;

import com.tiendaonline.pedidos.client.ItemAReservar;
import com.tiendaonline.pedidos.client.ReservaInventario;
import com.tiendaonline.pedidos.client.ServicioClientesClient;
import com.tiendaonline.pedidos.client.ServicioInventarioClient;
import com.tiendaonline.pedidos.domain.Pedido;
import com.tiendaonline.pedidos.exception.PedidoNoEncontradoException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import com.tiendaonline.pedidos.repository.PedidoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ServicioClientesClient clientesClient;
    private final ServicioInventarioClient inventarioClient;
    private final Clock clock;

    public Pedido obtener(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNoEncontradoException(pedidoId));
    }

    public List<Pedido> listarPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteIdOrderByIdAsc(clienteId);
    }

    /**
     * 1. verifica que el cliente exista
     * 2. guarda el pedido (se necesita su id para reservar)
     * 3. reserva el stock de todos los items en inventario
     * 4. agrega los items con el precio de la reserva y calcula el total
     * Si algo falla, la transaccion hace rollback y el pedido no se crea
     */
    @Transactional
    public Pedido crear(Long clienteId, List<ItemAReservar> items) {
        clientesClient.verificarExistencia(clienteId);

        Pedido pedido = pedidoRepository.saveAndFlush(Pedido.crear(clienteId, LocalDateTime.now(clock)));

        List<ReservaInventario> reservas;
        try {
            reservas = inventarioClient.reservar(pedido.getId(), items);
        } catch (ServicioNoDisponibleException ex) {
            // no sabemos si inventario llego a reservar, liberamos por las dudas (es idempotente)
            compensar(pedido.getId());
            throw ex;
        }

        for (ReservaInventario reserva : reservas) {
            pedido.agregarItem(reserva.productoId(), reserva.cantidad(), reserva.precioUnitario());
        }
        log.info("Pedido creado pedidoId={} clienteId={} montoTotal={}", pedido.getId(), clienteId,
                pedido.getMontoTotal());
        return pedido;
    }

    // Cancela un pedido CREADO o CONFIRMADO y libera su stock en inventario
    @Transactional
    public Pedido cancelar(Long pedidoId) {
        Pedido pedido = obtener(pedidoId);
        pedido.cancelar(LocalDateTime.now(clock));
        inventarioClient.liberarPorPedido(pedidoId);
        log.info("Pedido cancelado pedidoId={}", pedidoId);
        return pedido;
    }

    private void compensar(Long pedidoId) {
        try {
            inventarioClient.liberarPorPedido(pedidoId);
        } catch (RuntimeException ex) {
            log.error("No se pudo liberar el stock del pedido {} al compensar", pedidoId, ex);
        }
    }
}
