package com.tiendaonline.inventario.web;

import com.tiendaonline.inventario.exception.PedidoConReservasActivasException;
import com.tiendaonline.inventario.exception.RecursoNoEncontradoException;
import com.tiendaonline.inventario.exception.ReservaYaLiberadaException;
import com.tiendaonline.inventario.exception.SolicitudInvalidaException;
import com.tiendaonline.inventario.exception.StockInsuficienteException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones a respuestas (application/problem+json) con el formato "type, title, status, detail, instance" y propiedades extra segun el caso.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_BASE = "https://tiendaonline.com/problemas/";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "recurso-no-encontrado", "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ProblemDetail handleStockInsuficiente(StockInsuficienteException ex) {
        ProblemDetail problema = problema(HttpStatus.CONFLICT, "stock-insuficiente", "Stock insuficiente",
                ex.getMessage());
        problema.setProperty("faltantes", ex.getFaltantes());
        return problema;
    }

    @ExceptionHandler(ReservaYaLiberadaException.class)
    public ProblemDetail handleReservaYaLiberada(ReservaYaLiberadaException ex) {
        return problema(HttpStatus.CONFLICT, "reserva-ya-liberada", "Reserva ya liberada", ex.getMessage());
    }

    @ExceptionHandler(PedidoConReservasActivasException.class)
    public ProblemDetail handlePedidoConReservas(PedidoConReservasActivasException ex) {
        return problema(HttpStatus.CONFLICT, "pedido-con-reservas-activas", "Pedido con reservas activas",
                ex.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail handleSolicitudInvalida(SolicitudInvalidaException ex) {
        return problema(HttpStatus.BAD_REQUEST, "solicitud-invalida", "Solicitud invalida", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInesperado(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Error interno",
                "Ocurrio un error inesperado");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "campo", error.getField(),
                        "mensaje", String.valueOf(error.getDefaultMessage())))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "validacion", "Datos invalidos",
                "La solicitud tiene " + errores.size() + " error(es) de validacion");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<Map<String, String>> errores = ex.getAllValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(error -> Map.of(
                                "campo", String.valueOf(resultado.getMethodParameter().getParameterName()),
                                "mensaje", String.valueOf(error.getDefaultMessage()))))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "validacion", "Datos invalidos",
                "La solicitud tiene " + errores.size() + " error(es) de validacion");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    private static ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setType(URI.create(TYPE_BASE + tipo));
        problema.setTitle(titulo);
        return problema;
    }
}
