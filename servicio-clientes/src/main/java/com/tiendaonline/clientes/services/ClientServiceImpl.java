package com.tiendaonline.clientes.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tiendaonline.clientes.dto.ClientDTO;
import com.tiendaonline.clientes.exceptions.ClientNotFoundException;
import com.tiendaonline.clientes.exceptions.DuplicatedEmailException;
import com.tiendaonline.clientes.flags.FeatureFlags;
import com.tiendaonline.clientes.mapper.ClientMapper;
import com.tiendaonline.clientes.repositories.ClientRepository;
import com.tiendaonline.clientes.services.utils.PiiMasker;

@Service 
public class ClientServiceImpl implements ClientService {

    private ClientRepository clientRepository;
    private FeatureFlags flags;
    private PiiMasker masker;

    @Autowired 
    ClientServiceImpl(ClientRepository repository, FeatureFlags flags, PiiMasker masker) {
        this.clientRepository = repository;
        this.flags = flags;
        this.masker = masker;
    }
    
    @Override
    public ClientDTO registerClient(ClientDTO client) throws DuplicatedEmailException {
        if(client.getId() != null) throw new IllegalArgumentException("A new client can not have id");
        if(clientRepository.existsByEmail(client.getEmail())) throw new DuplicatedEmailException();
        return ClientMapper.toDto(clientRepository.save(ClientMapper.toEntity(client)));
    }

    @Override
    public ClientDTO searchById(Long id) throws ClientNotFoundException{
        ClientDTO client = clientRepository.findById(id).map(ClientMapper::toDto).orElseThrow(ClientNotFoundException::new);
        if(flags.isMaskPiiEnabled()) client = masker.maskClient(client);
        return client;
    }
    
}
