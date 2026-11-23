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
import com.tiendaonline.clientes.flags.FeatureFlags;
import com.tiendaonline.clientes.repositories.ClientRepository;
import com.tiendaonline.clientes.services.utils.PiiMasker;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {

    @Mock
    FeatureFlags flags;
    @Mock
    PiiMasker masker;
    @Mock
    ClientRepository repository;
    @InjectMocks
    ClientServiceImpl service;

    ClientDTO getUnsavedTestSubjectDTO() {
        return ClientDTO.builder().name("Test").surname("Subject").email("test@test.test").build();
    }

    ClientDTO getSavedTestSubjectDTO() {
        return ClientDTO.builder().name("Test").surname("Subject").email("test@test.test").id(1L).build();
    }

    ClientDTO getMaskedTestSubjectDTO() {
        return ClientDTO.builder().name("T***").surname("S***").email("****@****.test").build();
    }

    Client getSavedTestSubjectEntity() {
        return Client.builder().id(1L).name("Test").surname("Subject").email("test@test.test").build();
    }

    Client getUnsavedTestSubjectEntity() {
        return Client.builder().name("Test").surname("Subject").email("test@test.test").build();
    }

    @Test
    void searchById_throws_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> service.searchById(1L));
    }

    @Test
    void searchById_flagInactive_returnsDataUnmasked() {
        when(repository.findById(1L)).thenReturn(Optional.of(this.getSavedTestSubjectEntity()));
        when(flags.isMaskPiiEnabled()).thenReturn(false);

        assertEquals(getSavedTestSubjectDTO(), service.searchById(1L));

    }

    @Test
    void searchById_flagActive_returnsMaskedData() {
        when(repository.findById(1L)).thenReturn(Optional.of(getSavedTestSubjectEntity()));
        when(flags.isMaskPiiEnabled()).thenReturn(true);
        when(masker.maskClient(getSavedTestSubjectDTO())).thenReturn(getMaskedTestSubjectDTO());

        assertEquals(getMaskedTestSubjectDTO(), service.searchById(1L));
    }

    @Test
    void registerClient_throwsDuplicatedEmail() {
        when(repository.existsByEmail("test@test.test")).thenReturn(true);

        assertThrows(DuplicatedEmailException.class,
                () -> service.registerClient(getUnsavedTestSubjectDTO()));
        verify(repository, never()).save(any());
    }

    @Test
    void registerClient_savesAndReturnsDtoUnmasked() {
        when(repository.save(getUnsavedTestSubjectEntity())).thenReturn(getSavedTestSubjectEntity());

        ClientDTO result = service.registerClient(getUnsavedTestSubjectDTO());

        assertEquals(getSavedTestSubjectDTO(), result);
        verify(repository).save(getUnsavedTestSubjectEntity());
    }

}
