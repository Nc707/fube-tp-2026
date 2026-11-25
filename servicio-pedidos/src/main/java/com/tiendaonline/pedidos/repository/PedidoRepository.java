package com.tiendaonline.pedidos.repository;

import com.tiendaonline.pedidos.domain.EstadoPedido;
import com.tiendaonline.pedidos.domain.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Pedido> findById(Long id);

    @EntityGraph(attributePaths = "items")
    List<Pedido> findByClienteIdOrderByIdAsc(Long clienteId);

    // La usa el batch: pedidos en un estado y creados antes de cierta fecha
    List<Pedido> findByEstadoAndFechaCreacionBefore(EstadoPedido estado, LocalDateTime limite);
}
