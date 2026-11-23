package com.tiendaonline.clientes.services.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tiendaonline.clientes.dto.ClientDTO;

@Component
public class PiiMaskerImpl implements PiiMasker {

    private static final String WORD_MASK = "***";
    private static final String EMAIL_MASK = "****";

    @Override
    public ClientDTO maskClient(ClientDTO client) {
        if (client == null) {
            return null;
        }
        return client.toBuilder()
                .name(maskWords(client.getName()))
                .surname(maskWords(client.getSurname()))
                .email(maskEmail(client.getEmail()))
                .build();
    }

    private String maskWords(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return Arrays.stream(text.trim().split("\\s+"))
                .map(this::maskWord)
                .collect(Collectors.joining(" "));
    }

    private String maskWord(String word) {
        return word.charAt(0) + WORD_MASK;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        String[] parts = email.trim().split("@", -1);
        if (parts.length != 2) {
            return EMAIL_MASK;
        }
        return EMAIL_MASK + "@" + EMAIL_MASK + extensionOf(parts[1]);
    }

    private String extensionOf(String domain) {
        String[] labels = domain.split("\\.", -1);
        return labels.length > 1 ? "." + labels[labels.length - 1] : "";
    }

}