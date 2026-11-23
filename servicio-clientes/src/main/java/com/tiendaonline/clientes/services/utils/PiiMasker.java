package com.tiendaonline.clientes.services.utils;

import com.tiendaonline.clientes.dto.ClientDTO;

public interface PiiMasker {
    
    ClientDTO maskClient(ClientDTO client);
}
