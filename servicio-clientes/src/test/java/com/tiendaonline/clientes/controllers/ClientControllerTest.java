package com.tiendaonline.clientes.controllers;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.tiendaonline.clientes.dto.ClientDTO;
import com.tiendaonline.clientes.exceptions.ClientNotFoundException;
import com.tiendaonline.clientes.exceptions.DuplicatedEmailException;
import com.tiendaonline.clientes.services.ClientService;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ClientService clientService;

    ClientDTO getSavedTestSubjectDTO() {
        return ClientDTO.builder().id(1L).name("Test").surname("Subject").email("test@test.test").build();
    }

    private static final String VALID_BODY = """
            {"name":"Test","surname":"Subject","email":"test@test.test"}""";

    @Test
    void register_returns201WithLocation() throws Exception {
        when(clientService.registerClient(any(ClientDTO.class))).thenReturn(getSavedTestSubjectDTO());

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/clientes/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test"));
    }

    @Test
    void register_invalidBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","surname":"Subject","email":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(clientService);
    }

    @Test
    void register_ignoresIdSentInBody() throws Exception {
        when(clientService.registerClient(any(ClientDTO.class))).thenReturn(getSavedTestSubjectDTO());

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":99,"name":"Test","surname":"Subject","email":"test@test.test"}"""))
                .andExpect(status().isCreated());

        ArgumentCaptor<ClientDTO> captor = ArgumentCaptor.forClass(ClientDTO.class);
        verify(clientService).registerClient(captor.capture());
        assertNull(captor.getValue().getId());
    }

    @Test
    void register_duplicatedEmail_returns409() throws Exception {
        when(clientService.registerClient(any(ClientDTO.class))).thenThrow(new DuplicatedEmailException());

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void findById_returns200() throws Exception {
        when(clientService.searchById(1L)).thenReturn(getSavedTestSubjectDTO());

        mockMvc.perform(get("/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.test"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(clientService.searchById(1L)).thenThrow(new ClientNotFoundException());

        mockMvc.perform(get("/clientes/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/clientes/abc"))
                .andExpect(status().isBadRequest());
    }
}