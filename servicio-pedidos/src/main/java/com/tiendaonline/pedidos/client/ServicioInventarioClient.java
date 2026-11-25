package com.tiendaonline.pedidos.client;

import com.tiendaonline.pedidos.exception.ProductoNoEncontradoException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import com.tiendaonline.pedidos.exception.SolicitudInvalidaException;
import com.tiendaonline.pedidos.exception.StockInsuficienteException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class ServicioInventarioClient {

    private static final String SERVICIO = "servicio-inventario";
    private static final String TIPO_STOCK_INSUFICIENTE = "/stock-insuficiente";

    private final RestClient restClient;

    public ServicioInventarioClient(@Qualifier("inventarioRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    private record ReservarRequest(Long pedidoId, List<ItemAReservar> items) {
    }

    private record ReservasResponse(Long pedidoId, List<ReservaInventario> reservas) {
    }

    private record ProblemaInventario(String type, String detail, List<StockInsuficienteException.Faltante> faltantes) {
    }

    /**
     * reserva todos los items del pedido (inventario reserva todo o nada)
     *
     * @throws StockInsuficienteException si algun item no alcanza (no se reservo ninguno)
     * @throws ProductoNoEncontradoException si algun producto no existe (no se reservo ninguno)
     * @throws ServicioNoDisponibleException si inventario no respondio o fallo , puede haber reservado o no
     */
    public List<ReservaInventario> reservar(Long pedidoId, List<ItemAReservar> items) {
        try {
            ReservasResponse respuesta = restClient.post()
                    .uri("/api/v1/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReservarRequest(pedidoId, items))
                    .retrieve()
                    .body(ReservasResponse.class);
            return respuesta == null || respuesta.reservas() == null ? List.of() : respuesta.reservas();
        } catch (HttpClientErrorException.Conflict ex) {
            ProblemaInventario problema = leerProblema(ex);
            if (problema != null && problema.type() != null && problema.type().endsWith(TIPO_STOCK_INSUFICIENTE)
                    && problema.faltantes() != null) {
                log.info("Stock insuficiente pedidoId={} faltantes={}", pedidoId, problema.faltantes());
                throw new StockInsuficienteException(problema.faltantes());
            }
            throw errorInesperado("reservar", pedidoId, ex);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ProductoNoEncontradoException(detalle(ex));
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new SolicitudInvalidaException(detalle(ex));
        } catch (RestClientResponseException ex) {
            throw errorInesperado("reservar", pedidoId, ex);
        } catch (RestClientException ex) {
            throw sinRespuesta("reservar", pedidoId, ex);
        }
    }

    public void liberarPorPedido(Long pedidoId) {
        try {
            restClient.post()
                    .uri(uri -> uri.path("/api/v1/reservas/liberacion").queryParam("pedidoId", pedidoId).build())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Reservas liberadas en inventario pedidoId={}", pedidoId);
        } catch (RestClientResponseException ex) {
            throw errorInesperado("liberar", pedidoId, ex);
        } catch (RestClientException ex) {
            throw sinRespuesta("liberar", pedidoId, ex);
        }
    }

    private ServicioNoDisponibleException errorInesperado(String operacion, Long pedidoId,
                                                          RestClientResponseException ex) {
        log.warn("servicio-inventario respondio {} al {} pedidoId={}", ex.getStatusCode(), operacion, pedidoId);
        return new ServicioNoDisponibleException(SERVICIO, "respondio " + ex.getStatusCode(), ex);
    }

    private ServicioNoDisponibleException sinRespuesta(String operacion, Long pedidoId, RestClientException ex) {
        log.warn("servicio-inventario no respondio al {} pedidoId={}: {}", operacion, pedidoId, ex.getMessage());
        return new ServicioNoDisponibleException(SERVICIO, ex.getMessage(), ex);
    }

    private static String detalle(RestClientResponseException ex) {
        ProblemaInventario problema = leerProblema(ex);
        return problema != null && problema.detail() != null ? problema.detail() : ex.getStatusText();
    }

    private static ProblemaInventario leerProblema(RestClientResponseException ex) {
        try {
            return ex.getResponseBodyAs(ProblemaInventario.class);
        } catch (RuntimeException noSePudoLeer) {
            return null;
        }
    }
}
