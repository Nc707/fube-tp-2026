package com.tiendaonline.inventario.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventarioOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TiendaOnline - servicio-inventario")
                .description("Consulta de disponibilidad de stock y gestion de reservas de stock por pedido.")
                .version("v1"));
    }
}
