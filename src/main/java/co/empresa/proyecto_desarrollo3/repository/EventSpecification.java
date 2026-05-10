package co.empresa.proyecto_desarrollo3.repository;

import co.empresa.proyecto_desarrollo3.dto.request.EventSearchRequest;
import co.empresa.proyecto_desarrollo3.model.Event;
import co.empresa.proyecto_desarrollo3.model.enums.EventStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    private EventSpecification() {}

    /**
     * Construye un Specification dinámico a partir de los filtros
     * del EventSearchRequest. Solo aplica los filtros que no son nulos.
     *
     * Siempre incluye el filtro de status = PUBLISHED, ya que esta
     * especificación es exclusiva para el catálogo público.
     */
    public static Specification<Event> fromSearchRequest(EventSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Siempre filtrar solo eventos publicados
            predicates.add(cb.equal(root.get("status"), EventStatus.PUBLISHED));

            // Siempre mostrar solo eventos con fecha futura
            predicates.add(cb.greaterThan(root.get("eventDate"), LocalDateTime.now()));

            // Filtro por palabra clave (nombre o descripción)
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            // Filtro por ubicación (búsqueda parcial)
            if (request.getLocation() != null && !request.getLocation().isBlank()) {
                String pattern = "%" + request.getLocation().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("location")), pattern));
            }

            // Filtro por fecha desde
            if (request.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("eventDate"),
                        request.getDateFrom().atStartOfDay()
                ));
            }

            // Filtro por fecha hasta
            if (request.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("eventDate"),
                        request.getDateTo().atTime(23, 59, 59)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
