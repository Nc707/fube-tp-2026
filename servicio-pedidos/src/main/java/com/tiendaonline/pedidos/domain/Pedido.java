package com.tiendaonline.pedidos.domain;

import com.tiendaonline.pedidos.exception.TransicionEstadoInvalidaException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pedidos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemPedido> items = new ArrayList<>();

    // crea un pedido nuevo en estado CREADO, sin items y con monto total cero
    public static Pedido crear(Long clienteId, LocalDateTime now) {
        if (clienteId == null) {
            throw new IllegalArgumentException("El pedido debe tener un cliente");
        }
        Pedido pedido = new Pedido();
        pedido.clienteId = clienteId;
        pedido.estado = EstadoPedido.CREADO;
        pedido.montoTotal = BigDecimal.ZERO;
        pedido.fechaCreacion = now;
        pedido.fechaActualizacion = now;
        return pedido;
    }

    /**
     * Agrega un item con el precio unitario devuelto por servicio-inventario al reservar,
     * y recalcula el monto total. Solo se permite mientras el pedido esta CREADO.
     */
    public ItemPedido agregarItem(Long productoId, int cantidad, BigDecimal precioUnitario) {
        if (estado != EstadoPedido.CREADO) {
            throw new IllegalStateException("Solo se pueden agregar items a un pedido en estado CREADO");
        }
        if (productoId == null) {
            throw new IllegalArgumentException("El item debe tener un producto");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo");
        }
        ItemPedido item = new ItemPedido(this, productoId, cantidad, precioUnitario);
        items.add(item);
        montoTotal = calcularTotal();
        return item;
    }

    public void confirmar(LocalDateTime now) {
        cambiarEstado(EstadoPedido.CONFIRMADO, Set.of(EstadoPedido.CREADO), now);
    }

    public void cancelar(LocalDateTime now) {
        cambiarEstado(EstadoPedido.CANCELADO, Set.of(EstadoPedido.CREADO, EstadoPedido.CONFIRMADO), now);
    }

    public void expirar(LocalDateTime now) {
        cambiarEstado(EstadoPedido.EXPIRADO, Set.of(EstadoPedido.CREADO), now);
    }

    public List<ItemPedido> getItems() {
        return Collections.unmodifiableList(items);
    }

    private void cambiarEstado(EstadoPedido destino, Set<EstadoPedido> origenesValidos, LocalDateTime now) {
        if (!origenesValidos.contains(estado)) {
            throw new TransicionEstadoInvalidaException(id, estado, destino);
        }
        estado = destino;
        fechaActualizacion = now;
    }

    private BigDecimal calcularTotal() {
        return items.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
