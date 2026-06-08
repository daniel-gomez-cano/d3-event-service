package co.empresa.proyecto_desarrollo3.dto.response;

import co.empresa.proyecto_desarrollo3.model.TicketType;
import java.math.BigDecimal;

/**
 * Respuesta del endpoint interno GET /api/internal/ticket-types/{id}.
 * Consumido por el order-service para verificar precio y disponibilidad
 * antes de agregar un ítem al carrito.
 */
public record TicketTypeInfoResponse(
        Long id,
        String name,
        BigDecimal price,
        int remainingCapacity,
        boolean active,
        Long eventId
) {
    public static TicketTypeInfoResponse fromEntity(TicketType tt) {
        return new TicketTypeInfoResponse(
                tt.getId(),
                tt.getName(),
                tt.getPrice(),
                tt.remainingStock(),
                tt.getActive(),
                tt.getEvent().getId()
        );
    }
}