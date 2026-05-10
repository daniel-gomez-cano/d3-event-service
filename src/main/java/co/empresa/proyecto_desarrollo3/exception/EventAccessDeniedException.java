package co.empresa.proyecto_desarrollo3.exception;

public class EventAccessDeniedException extends RuntimeException {
    public EventAccessDeniedException(Long eventId) {
        super("No tienes permiso para modificar el evento con id: " + eventId);
    }
}
