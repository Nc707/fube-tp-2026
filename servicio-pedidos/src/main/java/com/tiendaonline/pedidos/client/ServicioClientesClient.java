package com.tiendaonline.pedidos.client;

import com.tiendaonline.pedidos.exception.ClienteNoEncontradoException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// lamadas sincronicas a servicio-clientes
@Slf4j
@Component
public class ServicioClientesClient {

    private static final String SERVICIO = "servicio-clientes";

    private final RestClient restClient;

    public ServicioClientesClient(@Qualifier("clientesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * verifica que el cliente exista, solo importa el codigo de respuesta: el cuerpo puede venir
     * enmascarado segun la flag mask-pii, y no es relevante aca
     */
    public void verificarExistencia(Long clienteId) {
        try {
            restClient.get()
                    .uri("/clients/{clienteId}", clienteId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Cliente inexistente clienteId={}", clienteId);
            throw new ClienteNoEncontradoException(clienteId);
        } catch (RestClientResponseException ex) {
            log.warn("servicio-clientes respondio {} al consultar clienteId={}", ex.getStatusCode(), clienteId);
            throw new ServicioNoDisponibleException(SERVICIO, "respondio " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.warn("servicio-clientes no respondio al consultar clienteId={}: {}", clienteId, ex.getMessage());
            throw new ServicioNoDisponibleException(SERVICIO, ex.getMessage(), ex);
        }
    }
}
