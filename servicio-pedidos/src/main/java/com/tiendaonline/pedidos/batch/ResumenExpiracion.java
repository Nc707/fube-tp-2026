package com.tiendaonline.pedidos.batch;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

// Resumen de una corrida del job de expiracion
public record ResumenExpiracion(Long ejecucionId, String estado, long leidos, long expirados) {

    public static ResumenExpiracion de(JobExecution jobExecution) {
        long leidos = 0;
        long expirados = 0;
        for (StepExecution step : jobExecution.getStepExecutions()) {
            leidos += step.getReadCount();
            expirados += step.getWriteCount();
        }
        return new ResumenExpiracion(jobExecution.getId(), jobExecution.getStatus().name(), leidos, expirados);
    }
}
