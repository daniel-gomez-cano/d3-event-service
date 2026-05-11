package co.empresa.proyecto_desarrollo3.controller;

import co.empresa.proyecto_desarrollo3.dto.request.CreateEventRequest;
import co.empresa.proyecto_desarrollo3.dto.request.EventSearchRequest;
import co.empresa.proyecto_desarrollo3.dto.request.ReleaseTicketRequest;
import co.empresa.proyecto_desarrollo3.dto.request.ReserveTicketRequest;
import co.empresa.proyecto_desarrollo3.dto.response.EventResponse;
import co.empresa.proyecto_desarrollo3.dto.response.PagedResponse;
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

    // ── US-03: Búsqueda con filtros (público) ────────────────────────

    /**
     * GET /api/v1/events/search
     *
     * Búsqueda paginada con filtros opcionales.
     * Todos los parámetros son opcionales — sin filtros retorna
     * todos los eventos publicados próximos.
     *
     * Ejemplos:
     *   GET /api/v1/events/search
     *   GET /api/v1/events/search?keyword=rock
     *   GET /api/v1/events/search?location=bogota
     *   GET /api/v1/events/search?dateFrom=2025-06-01&dateTo=2025-06-30
     *   GET /api/v1/events/search?keyword=jazz&location=cali&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<EventResponse>> searchEvents(
            @ModelAttribute EventSearchRequest request) {
        return ResponseEntity.ok(eventService.searchEvents(request));
    }

    // ── US-01: Catálogo simple (público) ─────────────────────────────

    /**
     * GET /api/v1/events
     * Listado simple sin filtros ni paginación.
     * Mantenido por compatibilidad con US-01.
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getPublishedEvents() {
        return ResponseEntity.ok(eventService.getPublishedEvents());
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