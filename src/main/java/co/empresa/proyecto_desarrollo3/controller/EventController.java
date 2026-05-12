package co.empresa.proyecto_desarrollo3.controller;

import co.empresa.proyecto_desarrollo3.dto.request.CreateEventRequest;
import co.empresa.proyecto_desarrollo3.dto.request.EventSearchRequest;
import co.empresa.proyecto_desarrollo3.dto.request.ReleaseTicketRequest;
import co.empresa.proyecto_desarrollo3.dto.request.ReserveTicketRequest;
import co.empresa.proyecto_desarrollo3.dto.response.EventResponse;
import co.empresa.proyecto_desarrollo3.dto.response.EventListItemResponse;
import co.empresa.proyecto_desarrollo3.dto.response.PagedResponse;
import co.empresa.proyecto_desarrollo3.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // ── US-03: Listado con filtros (público) ────────────────────────

    /**
     * GET /api/v1/events
     *
     * Búsqueda paginada con filtros opcionales.
     * Sin filtros de fecha, retorna todos los eventos publicados.
     *
     * Paginación: page (default 1), limit (default 12, max 50)
     *
     * Ejemplos:
     *   GET /api/v1/events
     *   GET /api/v1/events?keyword=rock
     *   GET /api/v1/events?location=bogota
     *   GET /api/v1/events?dateFrom=2025-06-01&dateTo=2025-06-30
     *   GET /api/v1/events?keyword=jazz&location=cali&page=1&limit=12
     */
    @GetMapping
    public ResponseEntity<PagedResponse<EventListItemResponse>> searchEvents(
            @ModelAttribute EventSearchRequest request) {
        return ResponseEntity.ok(eventService.searchEvents(request));
    }

    /**
     * GET /api/v1/events/search
     * Alias temporal para compatibilidad hacia atrás.
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<EventListItemResponse>> searchEventsAlias(
            @ModelAttribute EventSearchRequest request) {
        return ResponseEntity.ok(eventService.searchEvents(request));
    }

    // ── US-03: Detalle de evento (público) ───────────────────────────

    /**
     * GET /api/v1/events/{id}
     *
     * Retorna el detalle completo de un evento:
     * - Información general (nombre, fecha, lugar, imagen)
     * - Tipos de boleta activos con precio y cupos restantes
     *
        * Solo eventos en estado PUBLISHED son visibles.
     * Retorna 404 si el evento no existe o no está publicado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getPublishedEventById(id));
    }

    // ── US-01: Crear evento (organizador) ────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('EVENT_CREATOR')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String organizerKeycloakId = jwt.getSubject();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createDraft(request, organizerKeycloakId));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('EVENT_CREATOR')")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(eventService.publishEvent(id, jwt.getSubject()));
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasRole('EVENT_CREATOR')")
    public ResponseEntity<List<EventResponse>> getMyEvents(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(eventService.getMyEvents(jwt.getSubject()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EVENT_CREATOR')")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(eventService.cancelEvent(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/reserve")
    @PreAuthorize("hasRole('ORDER_SERVICE')")
    public ResponseEntity<EventResponse> reserveTickets(
            @PathVariable Long id,
            @Valid @RequestBody ReserveTicketRequest request) {

        return ResponseEntity.ok(eventService.reserveTickets(id, request));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('ORDER_SERVICE')")
    public ResponseEntity<EventResponse> releaseTickets(
            @PathVariable Long id,
            @Valid @RequestBody ReleaseTicketRequest request) {

        return ResponseEntity.ok(eventService.releaseTickets(id, request));
    }
}