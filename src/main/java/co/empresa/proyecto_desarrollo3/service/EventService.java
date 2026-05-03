package co.empresa.proyecto_desarrollo3.service;


import co.empresa.proyecto_desarrollo3.dto.request.CreateEventRequest;
import co.empresa.proyecto_desarrollo3.dto.response.EventResponse;
import co.empresa.proyecto_desarrollo3.exception.EventAccessDeniedException;
import co.empresa.proyecto_desarrollo3.exception.EventNotFoundException;
import co.empresa.proyecto_desarrollo3.model.Event;
import co.empresa.proyecto_desarrollo3.model.TicketType;
import co.empresa.proyecto_desarrollo3.model.enums.EventStatus;
import co.empresa.proyecto_desarrollo3.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // ── US-01: Crear y publicar evento ───────────────────────────────

    /**
     * Crea un evento en estado DRAFT y lo publica inmediatamente.
     * El organizerKeycloakId se extrae del JWT en el controller,
     * nunca del body del request.
     */
    @Transactional
    public EventResponse createAndPublish(CreateEventRequest request, String organizerKeycloakId) {

        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setImageUrl(request.getImageUrl());
        event.setMaxCapacity(request.getMaxCapacity());
        event.setOrganizerKeycloakId(organizerKeycloakId);
        event.setStatus(EventStatus.PUBLISHED);

        // Tipo de boleta por defecto (requerido en US-01)
        int ticketQuantity = request.getDefaultTicketQuantity() != null
                ? request.getDefaultTicketQuantity()
                : request.getMaxCapacity();

        TicketType defaultType = new TicketType(
                event,
                request.getDefaultTicketTypeName(),
                request.getDefaultTicketPrice(),
                ticketQuantity
        );

        event.getTicketTypes().add(defaultType);

        Event saved = eventRepository.save(event);
        return EventResponse.fromEntity(saved);
    }

    // ── Catálogo público ─────────────────────────────────────────────

    /**
     * Retorna todos los eventos publicados con fecha futura.
     * Accesible sin autenticación.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getPublishedEvents() {
        return eventRepository
                .findUpcomingPublishedEvents(LocalDateTime.now())
                .stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retorna el detalle de un evento por id.
     * Solo eventos PUBLISHED son visibles públicamente.
     */
    @Transactional(readOnly = true)
    public EventResponse getPublishedEventById(Long id) {
        Event event = eventRepository.findById(id)
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException(id));
        return EventResponse.fromEntity(event);
    }

    // ── Gestión del organizador ──────────────────────────────────────

    /**
     * Retorna todos los eventos del organizador autenticado.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(String organizerKeycloakId) {
        return eventRepository
                .findByOrganizerKeycloakId(organizerKeycloakId)
                .stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Verifica que el evento pertenece al organizador antes de
     * cualquier operación de modificación.
     */
    private Event findOwnedEvent(Long eventId, String organizerKeycloakId) {
        return eventRepository
                .findByIdAndOrganizerKeycloakId(eventId, organizerKeycloakId)
                .orElseThrow(() -> {
                    // Primero verificar si el evento existe en absoluto
                    if (!eventRepository.existsById(eventId)) {
                        return new EventNotFoundException(eventId);
                    }
                    // Existe pero no pertenece a este organizador
                    return new EventAccessDeniedException(eventId);
                });
    }

    /**
     * Cancela un evento del organizador. Usado en US-08.
     */
    @Transactional
    public EventResponse cancelEvent(Long eventId, String organizerKeycloakId) {
        Event event = findOwnedEvent(eventId, organizerKeycloakId);
        event.setStatus(EventStatus.CANCELLED);
        return EventResponse.fromEntity(eventRepository.save(event));
    }
}
