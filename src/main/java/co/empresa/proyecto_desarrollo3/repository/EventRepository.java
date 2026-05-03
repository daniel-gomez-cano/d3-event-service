package co.empresa.proyecto_desarrollo3.repository;


import co.empresa.proyecto_desarrollo3.model.Event;
import co.empresa.proyecto_desarrollo3.model.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Catálogo público: solo eventos publicados
    List<Event> findByStatus(EventStatus status);

    // Eventos de un organizador específico (por su keycloakId)
    List<Event> findByOrganizerKeycloakId(String organizerKeycloakId);

    // Evento por id y organizador — usado en @PreAuthorize para
    // verificar que el evento pertenece al organizador autenticado
    Optional<Event> findByIdAndOrganizerKeycloakId(Long id, String organizerKeycloakId);

    // Eventos publicados de un organizador
    List<Event> findByOrganizerKeycloakIdAndStatus(String organizerKeycloakId, EventStatus status);

    // Eventos próximos (fecha futura) visibles en el catálogo
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.eventDate > :now ORDER BY e.eventDate ASC")
    List<Event> findUpcomingPublishedEvents(@Param("now") LocalDateTime now);

    // Verificar si hay cupo antes de vender (para Order Service)
    @Query("SELECT e.soldTickets < e.maxCapacity FROM Event e WHERE e.id = :id")
    boolean hasAvailableCapacity(@Param("id") Long id);
}