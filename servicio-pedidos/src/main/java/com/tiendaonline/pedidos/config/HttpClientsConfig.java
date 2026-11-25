package com.tiendaonline.pedidos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ServiciosExternosProperties.class)
public class HttpClientsConfig {

    @Bean
    public RestClient clientesRestClient(RestClient.Builder builder, ServiciosExternosProperties properties) {
        return builder.clone()
                .baseUrl(properties.clientes().url())
                .requestFactory(requestFactory(properties))
                .build();
    }

    @Bean
    public RestClient inventarioRestClient(RestClient.Builder builder, ServiciosExternosProperties properties) {
        return builder.clone()
                .baseUrl(properties.inventario().url())
                .requestFactory(requestFactory(properties))
                .build();
    }

    private static ClientHttpRequestFactory requestFactory(ServiciosExternosProperties properties) {
        return ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.http().connectTimeout())
                .withReadTimeout(properties.http().readTimeout()));
    }
}
