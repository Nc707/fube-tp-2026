package com.tiendaonline.clientes.mapper;

import com.tiendaonline.clientes.dto.ClientDTO;
import com.tiendaonline.clientes.entities.Client;

public abstract class ClientMapper {

    public static ClientDTO toDto(Client entity) {
        return ClientDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .surname(entity.getSurname())
                .email(entity.getEmail())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static Client toEntity(ClientDTO dto) {
        return Client.builder()
                .id(dto.getId())
                .name(dto.getName())
                .surname(dto.getSurname())
                .email(dto.getEmail())
                .build();
    }
}