package com.tiendaonline.clientes.services;

import com.tiendaonline.clientes.dto.ClientDTO;
import com.tiendaonline.clientes.exceptions.ClientNotFoundException;
import com.tiendaonline.clientes.exceptions.DuplicatedEmailException;

public interface ClientService {
    
    public ClientDTO registerClient(ClientDTO client) throws DuplicatedEmailException;
    public ClientDTO searchById(Long id) throws ClientNotFoundException;
}
