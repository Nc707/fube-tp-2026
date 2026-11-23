package com.tiendaonline.clientes.services.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.tiendaonline.clientes.dto.ClientDTO;

class PiiMaskerImplTest {

    private final PiiMasker masker = new PiiMaskerImpl();

    private static final Instant CREATED_AT = Instant.parse("2026-11-21T12:00:00Z");

    ClientDTO getSavedTestSubjectDTO() {
        return ClientDTO.builder().id(1L).name("Juan").surname("Perez").email("juan@gmail.com")
                .createdAt(CREATED_AT).build();
    }

    ClientDTO getMaskedTestSubjectDTO() {
        return ClientDTO.builder().id(1L).name("J***").surname("P***").email("****@****.com")
                .createdAt(CREATED_AT).build();
    }

    @Test
    void maskClient_masksNameSurnameAndEmail() {
        assertEquals(getMaskedTestSubjectDTO(), masker.maskClient(getSavedTestSubjectDTO()));
    }

    @Test
    void maskClient_doesNotModifyOriginal() {
        ClientDTO original = getSavedTestSubjectDTO();

        masker.maskClient(original);

        assertEquals(getSavedTestSubjectDTO(), original);
    }

    @Test
    void maskClient_null() {
        assertNull(masker.maskClient(null));
    }

    @ParameterizedTest
    @CsvSource({
            "Test, T***",
            "Test Subject, T*** S***",
            "T, T***",
            "'  Test   Subject ', T*** S***"
    })
    void maskClient_name(String input, String expected) {
        assertEquals(expected, maskedName(input));
    }

    @ParameterizedTest
    @CsvSource({
            "test@subject.com, ****@****.com",
            "test@subject.com.ar, ****@****.ar",
            "testubject, ****",
            "test@subject, ****@****",
            "test@, ****@****",
            "test@subject@email.com, ****",
            "' test@subject.com ', ****@****.com"
    })
    void maskClient_email(String input, String expected) {
        assertEquals(expected, maskedEmail(input));
    }

    private String maskedName(String name) {
        return masker.maskClient(ClientDTO.builder().name(name).build()).getName();
    }

    private String maskedEmail(String email) {
        return masker.maskClient(ClientDTO.builder().email(email).build()).getEmail();
    }
}