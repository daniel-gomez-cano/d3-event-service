package co.empresa.proyecto_desarrollo3.service;


import co.empresa.proyecto_desarrollo3.dto.request.CreateEventRequest;
import co.empresa.proyecto_desarrollo3.dto.request.EventSearchRequest;
import co.empresa.proyecto_desarrollo3.dto.request.ReleaseTicketRequest;
import co.empresa.proyecto_desarrollo3.dto.request.ReserveTicketRequest;
import co.empresa.proyecto_desarrollo3.dto.response.EventListItemResponse;
import co.empresa.proyecto_desarrollo3.dto.response.EventResponse;
import co.empresa.proyecto_desarrollo3.dto.response.PagedResponse;
import co.empresa.proyecto_desarrollo3.exception.EventAccessDeniedException;
import co.empresa.proyecto_desarrollo3.exception.EventCapacityExceededException;
import co.empresa.proyecto_desarrollo3.exception.EventNotFoundException;
import co.empresa.proyecto_desarrollo3.exception.UnprocessableEntityException;
import co.empresa.proyecto_desarrollo3.model.Event;
import co.empresa.proyecto_desarrollo3.model.TicketType;
import co.empresa.proyecto_desarrollo3.model.enums.EventStatus;
import co.empresa.proyecto_desarrollo3.repository.EventRepository;
import co.empresa.proyecto_desarrollo3.repository.EventSpecification;
import co.empresa.proyecto_desarrollo3.repository.TicketTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public EventService(EventRepository eventRepository, TicketTypeRepository ticketTypeRepository) {
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    // ── US-01: Crear evento (borrador) ───────────────────────────────

    @Transactional
    public EventResponse createDraft(CreateEventRequest request, String organizerKeycloakId) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setImageUrl(request.getImageUrl());
        event.setMaxCapacity(request.getMaxCapacity());
        event.setOrganizerKeycloakId(organizerKeycloakId);
        event.setStatus(EventStatus.DRAFT);

        List<TicketType> ticketTypes = buildTicketTypes(request, event);
        event.getTicketTypes().addAll(ticketTypes);

        Event saved = eventRepository.save(event);
        return EventResponse.fromEntity(saved);
    }

    // ── US-01: Publicar evento (organizador) ─────────────────────────

    @Transactional
    public EventResponse publishEvent(Long eventId, String organizerKeycloakId) {
        Event event = findOwnedEvent(eventId, organizerKeycloakId);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException("Solo eventos en borrador pueden publicarse");
        }

        validatePublishRules(event);
        event.setStatus(EventStatus.PUBLISHED);

        return EventResponse.fromEntity(eventRepository.save(event));
    }

    // ── US-03: Búsqueda y catálogo público ───────────────────────────

    /**
     * Búsqueda paginada con filtros dinámicos.
         * Si no se envía ningún filtro de fecha, retorna todos los eventos
         * publicados, ordenados por fecha ascendente.
     */
    @Transactional(readOnly = true)
    public PagedResponse<EventListItemResponse> searchEvents(EventSearchRequest request) {
        validateSearchRequest(request);
        Specification<Event> spec = EventSpecification.fromSearchRequest(request);

        int pageIndex = request.getPage() - 1;

        Pageable pageable = PageRequest.of(
            pageIndex,
            request.getLimit(),
                Sort.by(Sort.Direction.ASC, "eventDate")
        );

        Page<EventListItemResponse> page = eventRepository
            .findAll(spec, pageable)
            .map(EventListItemResponse::fromEntity);

        return PagedResponse.fromPage(page);
    }

    /**
     * Detalle completo de un evento por id.
     * Incluye tipos de boleta activos con cupos restantes.
     * Solo retorna eventos en estado PUBLISHED.
     */
    @Transactional(readOnly = true)
    public EventResponse getPublishedEventById(Long id) {
        Event event = eventRepository.findById(id)
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException(id));
        return EventResponse.fromEntity(event);
    }

    // ── Gestión del organizador ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(String organizerKeycloakId) {
        return eventRepository
                .findByOrganizerKeycloakId(organizerKeycloakId)
                .stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventResponse cancelEvent(Long eventId, String organizerKeycloakId) {
        Event event = findOwnedEvent(eventId, organizerKeycloakId);
        event.setStatus(EventStatus.CANCELLED);
        return EventResponse.fromEntity(eventRepository.save(event));
    }

    // ── US-05: Reservar cupos (Order Service) ───────────────────────

    @Transactional
    public EventResponse reserveTickets(Long eventId, ReserveTicketRequest request) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("El evento no esta publicado");
        }

        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("El evento no esta disponible para venta");
        }

        TicketType ticketType = ticketTypeRepository
                .findByIdAndEventIdForUpdate(request.getTicketTypeId(), eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de boleta no encontrado para el evento"
                ));

        if (!Boolean.TRUE.equals(ticketType.getActive())) {
            throw new IllegalStateException("El tipo de boleta no esta activo");
        }

        int quantity = request.getQuantity();

        if (!ticketType.hasStock(quantity)) {
            throw new EventCapacityExceededException(ticketType.remainingStock());
        }

        if (!event.hasAvailableCapacity(quantity)) {
            throw new EventCapacityExceededException(event.remainingCapacity());
        }

        ticketType.incrementSoldQuantity(quantity);
        event.incrementSoldTickets(quantity);

        Event updatedEvent = eventRepository.saveAndFlush(event);
        return EventResponse.fromEntity(updatedEvent);
    }

    @Transactional
    public EventResponse releaseTickets(Long eventId, ReleaseTicketRequest request) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getStatus() == EventStatus.DRAFT) {
            throw new IllegalStateException("El evento no esta publicado");
        }

        TicketType ticketType = ticketTypeRepository
                .findByIdAndEventIdForUpdate(request.getTicketTypeId(), eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de boleta no encontrado para el evento"
                ));

        int quantity = request.getQuantity();

        ticketType.decrementSoldQuantity(quantity);
        event.decrementSoldTickets(quantity);

        Event updatedEvent = eventRepository.saveAndFlush(event);
        return EventResponse.fromEntity(updatedEvent);
    }

    private Event findOwnedEvent(Long eventId, String organizerKeycloakId) {
        return eventRepository
                .findByIdAndOrganizerKeycloakId(eventId, organizerKeycloakId)
                .orElseThrow(() -> {
                    if (!eventRepository.existsById(eventId)) {
                        return new EventNotFoundException(eventId);
                    }
                    return new EventAccessDeniedException(eventId);
                });
    }

    private List<TicketType> buildTicketTypes(CreateEventRequest request, Event event) {
        int totalQuantity = request.getTicketTypes().stream()
                .mapToInt(CreateEventRequest.TicketTypeRequest::getQuantity)
                .sum();

        if (totalQuantity > request.getMaxCapacity()) {
            throw new IllegalArgumentException(
                    "La suma de cantidades de boletas no puede superar el cupo maximo"
            );
        }

        return request.getTicketTypes().stream()
                .map(tt -> new TicketType(
                        event,
                        tt.getName(),
                        tt.getPrice(),
                        tt.getQuantity()
                ))
                .collect(Collectors.toList());
    }

    private void validatePublishRules(Event event) {
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La fecha del evento debe ser futura");
        }

        List<TicketType> activeTypes = event.getTicketTypes().stream()
                .filter(TicketType::getActive)
                .collect(Collectors.toList());

        if (activeTypes.isEmpty()) {
            throw new IllegalStateException("Debe existir al menos un tipo de boleta activo");
        }

        int totalQuantity = activeTypes.stream()
                .mapToInt(TicketType::getAvailableQuantity)
                .sum();

        if (totalQuantity > event.getMaxCapacity()) {
            throw new IllegalStateException(
                    "La suma de cantidades de boletas no puede superar el cupo maximo"
            );
        }
    }

    private void validateSearchRequest(EventSearchRequest request) {
        if (request.getPage() < 1) {
            throw new UnprocessableEntityException("page must be >= 1");
        }

        if (request.getLimit() < 1 || request.getLimit() > 50) {
            throw new UnprocessableEntityException("limit must be between 1 and 50");
        }

        if (request.getDateFrom() != null && request.getDateTo() != null
                && request.getDateFrom().isAfter(request.getDateTo())) {
            throw new UnprocessableEntityException("dateFrom must be before dateTo");
        }
    }
}