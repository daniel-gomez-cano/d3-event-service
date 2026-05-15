package co.empresa.proyecto_desarrollo3.exception;

public class EventCapacityExceededException extends RuntimeException {
    public EventCapacityExceededException(int remaining) {
        super("Cupo insuficiente. Disponibles: " + remaining);
    }
}