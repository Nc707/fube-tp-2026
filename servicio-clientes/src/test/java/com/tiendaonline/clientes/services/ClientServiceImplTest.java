package com.tiendaonline.clientes.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiendaonline.clientes.dto.ClientDTO;
import com.tiendaonline.clientes.entities.Client;
import com.tiendaonline.clientes.exceptions.ClientNotFoundException;
import com.tiendaonline.clientes.exceptions.DuplicatedEmailException;
import com.tiendaonline.clientes.repositories.ClientRepository;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {
    
    @Mock ClientRepository repository;
    @InjectMocks ClientServiceImpl service;

    ClientDTO getUnsavedTestSubject(){
        return ClientDTO.builder().name("Test").surname("Subject").email("test@test.test").build();
    }
    Client getSavedTestSubject(){
        return Client.builder().id(1L).name("Test").surname("Subject").email("test@test.test").build();
    }

    @Test 
    void searchById_throws_NotFound(){
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> service.searchById(1L));
    }

    @Test 
    void searchById_HappyPath(){
        when(repository.findById(1L)).thenReturn(Optional.of(this.getSavedTestSubject()));
        ClientDTO client = this.getUnsavedTestSubject();
        client.setId(1L);
        assertEquals(service.searchById(1L), client);

    }

    @Test 
    void registerClient_throws_duplicatedEmailException(){
        when(repository.existsByEmail("test@test.test")).thenReturn(true);
        ClientDTO dto = getUnsavedTestSubject();
        assertThrows(DuplicatedEmailException.class, () -> service.registerClient(dto));
        verify(repository, never()).save(any());
    }

   @Test
    void registerClient_HappyPath() {
    ClientDTO subject = getUnsavedTestSubject();
    when(repository.save(any(Client.class))).thenAnswer(inv -> {
        Client client = inv.getArgument(0);
        client.setId(1L);
        return client;
    });
    ClientDTO result;
    result = service.registerClient(subject);
    assertEquals(1L, result.getId());
    assertEquals(subject.getName(), result.getName());
    assertEquals(subject.getSurname(), result.getSurname());
    assertEquals(subject.getEmail(), result.getEmail());
    verify(repository).save(any(Client.class));
}

}
