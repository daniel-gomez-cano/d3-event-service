package co.empresa.proyecto_desarrollo3.repository;


import co.empresa.proyecto_desarrollo3.model.Event;
import co.empresa.proyecto_desarrollo3.model.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {  // ← AGREGADO para filtros dinámicos

    List<Event> findByStatus(EventStatus status);

    List<Event> findByOrganizerKeycloakId(String organizerKeycloakId);

    Optional<Event> findByIdAndOrganizerKeycloakId(Long id, String organizerKeycloakId);

    List<Event> findByOrganizerKeycloakIdAndStatus(String organizerKeycloakId, EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.eventDate > :now ORDER BY e.eventDate ASC")
    List<Event> findUpcomingPublishedEvents(@Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT e.soldTickets < e.maxCapacity FROM Event e WHERE e.id = :id")
    boolean hasAvailableCapacity(@Param("id") Long id);
}