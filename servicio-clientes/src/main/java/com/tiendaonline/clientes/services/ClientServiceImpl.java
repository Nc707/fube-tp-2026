package com.tiendaonline.clientes.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.tiendaonline.clientes.dto.ClientDTO;
import com.tiendaonline.clientes.entities.Client;
import com.tiendaonline.clientes.exceptions.ClientNotFoundException;
import com.tiendaonline.clientes.exceptions.DuplicatedEmailException;
import com.tiendaonline.clientes.mapper.ClientMapper;
import com.tiendaonline.clientes.repositories.ClientRepository;

public class ClientServiceImpl implements ClientService {

    private ClientRepository clientRepository;

    @Autowired 
    ClientServiceImpl(ClientRepository repository) {
        this.clientRepository = repository;
    }
    
    @Override
    public ClientDTO registerClient(ClientDTO client) throws DuplicatedEmailException {
        if(clientRepository.existsByEmail(client.getEmail())) throw new DuplicatedEmailException();
        Client saved = clientRepository.save(ClientMapper.toEntity(client));
        return ClientMapper.toDto(saved);
    }

    @Override
    public ClientDTO searchById(Long id) throws ClientNotFoundException{
        Optional<Client> optClient = clientRepository.findById(id);
        if(optClient.isEmpty()) throw new ClientNotFoundException();
        return ClientMapper.toDto(optClient.get());
    }
    
}
