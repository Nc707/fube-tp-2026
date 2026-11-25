package com.tiendaonline.pedidos.web;

import com.tiendaonline.pedidos.batch.ExpiracionPedidosScheduler;
import com.tiendaonline.pedidos.batch.ResumenExpiracion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "Batch", description = "Ejecucion manual del job de expiracion de pedidos")
public class BatchController {

    private final ExpiracionPedidosScheduler scheduler;

    @PostMapping("/expiracion-pedidos")
    @Operation(summary = "Ejecutar ahora el job de expiracion de pedidos",
            description = "El mismo job que corre todas las noches. Devuelve el resumen de la corrida.")
    public ResumenExpiracion ejecutar() throws Exception {
        return scheduler.ejecutar();
    }
}
