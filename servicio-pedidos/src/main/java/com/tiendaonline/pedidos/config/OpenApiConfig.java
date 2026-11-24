package com.tiendaonline.pedidos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pedidosOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TiendaOnline - servicio-pedidos")
                .description("Creacion, consulta y cancelacion de pedidos.")
                .version("v1"));
    }
}
