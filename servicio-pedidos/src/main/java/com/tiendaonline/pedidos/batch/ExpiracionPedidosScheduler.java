package com.tiendaonline.pedidos.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Lanza el job de expiracion todas las noches a las 3 AM 
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ExpiracionPedidosScheduler {

    private final JobLauncher jobLauncher;
    private final Job expiracionPedidosJob;

    @Scheduled(cron = "0 0 3 * * *")
    public void ejecutarNocturno() {
        try {
            ejecutar();
        } catch (Exception ex) {
            log.error("Fallo la corrida nocturna de expiracion de pedidos", ex);
        }
    }

    public ResumenExpiracion ejecutar() throws Exception {
        JobParameters parametros = new JobParametersBuilder()
                .addLong("inicio", System.currentTimeMillis())
                .toJobParameters();
        JobExecution ejecucion = jobLauncher.run(expiracionPedidosJob, parametros);
        return ResumenExpiracion.de(ejecucion);
    }
}
