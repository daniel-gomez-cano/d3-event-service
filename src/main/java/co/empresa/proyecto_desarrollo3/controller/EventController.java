package co.empresa.proyecto_desarrollo3.controller;

import co.empresa.proyecto_desarrollo3.dto.request.CreateEventRequest;
import co.empresa.proyecto_desarrollo3.dto.response.EventResponse;
import co.empresa.proyecto_desarrollo3.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // ── Endpoints públicos (sin autenticación) ───────────────────────

    /**
     * GET /api/v1/events
     * Catálogo público de eventos disponibles.
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getPublishedEvents() {
        return ResponseEntity.ok(eventService.getPublishedEvents());
    }

    /**
     * GET /api/v1/events/{id}
     * Detalle de un evento específico.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getPublishedEventById(id));
    }

    // ── Endpoints del organizador (ROLE_EVENT_CREATOR) ───────────────

    /**
     * POST /api/v1/events
     * Crea y publica un evento.
     * El organizerKeycloakId se extrae del JWT — nunca del body.
     */
    @PostMapping
    @PreAuthorize("hasRole('EVENT_CREATOR')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String organizerKeycloakId = jwt.getSubject();
        EventResponse response = eventService.createAndPublish(request, organizerKeycloakId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/events/my-events
     * Lista todos los eventos del organizador autenticado.
     */
    @GetMapping("/my-events")
    @PreAuthorize("hasRole('EVENT_CREATOR')")
    public ResponseEntity<List<EventResponse>> getMyEvents(
            @AuthenticationPrincipal Jwt jwt) {

        String organizerKeycloakId = jwt.getSubject();
        return ResponseEntity.ok(eventService.getMyEvents(organizerKeycloakId));
    }

    /**
     * PATCH /api/v1/events/{id}/cancel
     * Cancela un evento del organizador. Expande la lógica en US-08.
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EVENT_CREATOR')")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        String organizerKeycloakId = jwt.getSubject();
        return ResponseEntity.ok(eventService.cancelEvent(id, organizerKeycloakId));
    }
}

