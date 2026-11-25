package com.tiendaonline.pedidos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "servicios")
public record ServiciosExternosProperties(Servicio clientes, Servicio inventario, Http http) {

    public record Servicio(String url) {
    }

    public record Http(Duration connectTimeout, Duration readTimeout) {
    }
}
