package com.tiendaonline.clientes.mapper;

import com.tiendaonline.clientes.dto.ClientDTO;
import com.tiendaonline.clientes.entities.Client;

public abstract class ClientMapper {
    
    public static ClientDTO toDto(Client entity) {
        return new ClientDTO(entity.getId(),
                            entity.getName(), 
                            entity.getSurname(),
                            entity.getEmail());
    }
    public static Client toEntity(ClientDTO dto) {
        return new Client(dto.getId(), 
                        dto.getName(), 
                        dto.getSurname(), 
                        dto.getEmail());
    }
}
