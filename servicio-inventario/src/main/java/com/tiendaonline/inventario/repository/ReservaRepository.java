package com.tiendaonline.inventario.repository;

import com.tiendaonline.inventario.domain.EstadoReserva;
import com.tiendaonline.inventario.domain.Reserva;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Override
    @EntityGraph(attributePaths = "producto")
    Optional<Reserva> findById(Long id);

    @EntityGraph(attributePaths = "producto")
    List<Reserva> findByPedidoIdOrderByIdAsc(Long pedidoId);

    boolean existsByPedidoIdAndEstado(Long pedidoId, EstadoReserva estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.pedidoId = :pedidoId and r.estado = :estado order by r.id")
    List<Reserva> findByPedidoIdAndEstadoForUpdate(@Param("pedidoId") Long pedidoId,
                                                   @Param("estado") EstadoReserva estado);
}
