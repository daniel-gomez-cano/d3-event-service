package co.empresa.proyecto_desarrollo3.controller;

import co.empresa.proyecto_desarrollo3.dto.request.ReleaseTicketRequest;
import co.empresa.proyecto_desarrollo3.dto.request.ReserveTicketRequest;
import co.empresa.proyecto_desarrollo3.dto.response.TicketTypeInfoResponse;
import co.empresa.proyecto_desarrollo3.repository.TicketTypeRepository;
import co.empresa.proyecto_desarrollo3.service.EventService;
import jakarta.validation.Valid;

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
    private final EventService eventService;

    public EventInternalController(TicketTypeRepository ticketTypeRepository, EventService eventService) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventService = eventService;
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

    /** Llamado por order-service tras pago APPROVED */
    @PostMapping("/events/{id}/reserve")
    public ResponseEntity<Void> reserveTickets(
            @PathVariable Long id,
            @Valid @RequestBody ReserveTicketRequest request) {
        eventService.reserveTickets(id, request);
        return ResponseEntity.ok().build();
    }

    /** Llamado por order-service cuando se procesa un reembolso */
    @PostMapping("/events/{id}/release")
    public ResponseEntity<Void> releaseTickets(
            @PathVariable Long id,
            @Valid @RequestBody ReleaseTicketRequest request) {
        eventService.releaseTickets(id, request);
        return ResponseEntity.ok().build();
    }
}