package com.tiendaonline.inventario.service;

import com.tiendaonline.inventario.domain.Producto;
import com.tiendaonline.inventario.exception.ProductoNoEncontradoException;
import com.tiendaonline.inventario.repository.ProductoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;

    public Producto obtener(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new ProductoNoEncontradoException(productoId));
    }

    public List<Producto> listar() {
        return productoRepository.findAll(Sort.by("id"));
    }
}
