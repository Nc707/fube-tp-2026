package com.tiendaonline.pedidos.batch;

import com.tiendaonline.pedidos.client.ServicioInventarioClient;
import com.tiendaonline.pedidos.domain.EstadoPedido;
import com.tiendaonline.pedidos.domain.Pedido;
import com.tiendaonline.pedidos.repository.PedidoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


 //Cron nocturno: pasa a EXPIRADO los pedidos CREADO con mas de 24hs sin confirmar y libera su stock en servicio-inventario.

@Slf4j
@Configuration
public class ExpiracionPedidosJobConfig {

    private static final int HORAS_PARA_EXPIRAR = 24;
    private static final int TAMANIO_CHUNK = 10;

    @Bean
    public Job expiracionPedidosJob(JobRepository jobRepository, Step expirarPedidosStep) {
        return new JobBuilder("expiracionPedidosJob", jobRepository)
                .start(expirarPedidosStep)
                .listener(resumenListener())
                .build();
    }

    @Bean
    public Step expirarPedidosStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                   ListItemReader<Pedido> pedidosVencidosReader,
                                   ItemProcessor<Pedido, Pedido> expirarPedidoProcessor,
                                   ItemWriter<Pedido> expirarPedidoWriter) {
        return new StepBuilder("expirarPedidosStep", jobRepository)
                .<Pedido, Pedido>chunk(TAMANIO_CHUNK, transactionManager)
                .reader(pedidosVencidosReader)
                .processor(expirarPedidoProcessor)
                .writer(expirarPedidoWriter)
                .build();
    }

    // Reader: los pedidos CREADO con mas de 24hs. @StepScope hace que la consulta se haga en cada corrida
    @Bean
    @StepScope
    public ListItemReader<Pedido> pedidosVencidosReader(PedidoRepository pedidoRepository, Clock clock) {
        LocalDateTime limite = LocalDateTime.now(clock).minusHours(HORAS_PARA_EXPIRAR);
        return new ListItemReader<>(
                pedidoRepository.findByEstadoAndFechaCreacionBefore(EstadoPedido.CREADO, limite));
    }

    // Processor: cambia el estado del pedido a EXPIRADO
    @Bean
    public ItemProcessor<Pedido, Pedido> expirarPedidoProcessor(Clock clock) {
        return pedido -> {
            pedido.expirar(LocalDateTime.now(clock));
            return pedido;
        };
    }

    // Writer: libera el stock en inventario y guarda el pedido expirado
    @Bean
    public ItemWriter<Pedido> expirarPedidoWriter(ServicioInventarioClient inventarioClient,
                                                  PedidoRepository pedidoRepository) {
        return chunk -> {
            for (Pedido pedido : chunk) {
                inventarioClient.liberarPorPedido(pedido.getId());
                pedidoRepository.save(pedido);
                log.info("Pedido expirado pedidoId={}", pedido.getId());
            }
        };
    }

    // Al terminar la corrida, loguea el resumen 
    private JobExecutionListener resumenListener() {
        return new JobExecutionListener() {
            @Override
            public void afterJob(JobExecution jobExecution) {
                ResumenExpiracion resumen = ResumenExpiracion.de(jobExecution);
                log.info("Resumen expiracion de pedidos: estado={} leidos={} expirados={}",
                        resumen.estado(), resumen.leidos(), resumen.expirados());
            }
        };
    }
}
