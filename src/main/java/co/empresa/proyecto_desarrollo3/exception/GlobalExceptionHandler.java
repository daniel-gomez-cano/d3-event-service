package co.empresa.proyecto_desarrollo3.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Errores de validación (@Valid) ───────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Error de validación",
                errors,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }

    // ── Evento no encontrado ─────────────────────────────────────────
    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFound(EventNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── Acceso denegado al evento ────────────────────────────────────
    @ExceptionHandler(EventAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(EventAccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ── Cupo excedido ────────────────────────────────────────────────
    @ExceptionHandler(EventCapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleCapacityExceeded(EventCapacityExceededException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ── Error genérico de negocio ────────────────────────────────────
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ── Error de argumentos inválidos ───────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ── Contencion por bloqueo ──────────────────────────────────────
    @ExceptionHandler({PessimisticLockException.class, LockTimeoutException.class})
    public ResponseEntity<ErrorResponse> handleLockTimeout(RuntimeException ex) {
        return buildError(HttpStatus.CONFLICT, "Operacion en contencion, reintente");
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        ErrorResponse response = new ErrorResponse(
                status.value(),
                message,
                null,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(response);
    }

    // ── Clase interna de respuesta de error ──────────────────────────
    public static class ErrorResponse {
        private int status;
        private String message;
        private Map<String, String> fieldErrors;
        private LocalDateTime timestamp;

        public ErrorResponse(int status, String message,
                             Map<String, String> fieldErrors, LocalDateTime timestamp) {
            this.status = status;
            this.message = message;
            this.fieldErrors = fieldErrors;
            this.timestamp = timestamp;
        }

        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, String> getFieldErrors() { return fieldErrors; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}

