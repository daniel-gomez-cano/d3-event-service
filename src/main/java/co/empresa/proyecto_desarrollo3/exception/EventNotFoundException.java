package co.empresa.proyecto_desarrollo3.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(Long id) {
        super("Evento no encontrado con id: " + id);
    }
}
