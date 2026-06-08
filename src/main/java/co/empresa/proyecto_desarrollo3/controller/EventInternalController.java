package co.empresa.proyecto_desarrollo3.controller;

import co.empresa.proyecto_desarrollo3.dto.response.TicketTypeInfoResponse;
import co.empresa.proyecto_desarrollo3.repository.TicketTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoints internos del event-service — solo para comunicación entre microservicios.
 * No requieren JWT: en producción los protege la red interna del clúster / API Gateway.
 * No exponer estos endpoints externamente.
 */
@RestController
@RequestMapping("/api/internal")
public class EventInternalController {

    private final TicketTypeRepository ticketTypeRepository;

    public EventInternalController(TicketTypeRepository ticketTypeRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
    }

    /**
     * Consultado por el order-service en CartService.addItem().
     * Retorna precio y disponibilidad actual de un tipo de boleta.
     */
    @GetMapping("/ticket-types/{id}")
    public ResponseEntity<TicketTypeInfoResponse> getTicketTypeInfo(@PathVariable Long id) {
        return ticketTypeRepository.findById(id)
                .map(TicketTypeInfoResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tipo de boleta no encontrado: " + id));
    }
}