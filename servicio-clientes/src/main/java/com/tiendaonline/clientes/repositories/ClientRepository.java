package com.tiendaonline.clientes.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tiendaonline.clientes.entities.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {
    
    boolean existsByEmail(String email);
}
