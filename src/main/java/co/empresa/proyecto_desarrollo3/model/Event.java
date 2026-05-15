package co.empresa.proyecto_desarrollo3.model;

import co.empresa.proyecto_desarrollo3.model.enums.EventStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false)
    private String location;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "sold_tickets", nullable = false)
    private Integer soldTickets = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    // keycloakId del organizador dueño del evento.
    // Se extrae del JWT en la capa de servicio.
    @Column(name = "organizer_keycloak_id", nullable = false)
    private String organizerKeycloakId;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketType> ticketTypes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Event() {}

    // ── Lógica de negocio ────────────────────────────────────────────

    /**
     * Verifica si aún hay cupo disponible para la cantidad solicitada.
     * Usado por Order Service antes de reservar boletas.
     */
    public boolean hasAvailableCapacity(int quantity) {
        return (soldTickets + quantity) <= maxCapacity;
    }

    /**
     * Retorna los cupos restantes del evento.
     */
    public int remainingCapacity() {
        return maxCapacity - soldTickets;
    }

    /**
     * Incrementa el contador de boletas vendidas.
     * Lanza excepción si supera el cupo máximo.
     */
    public void incrementSoldTickets(int quantity) {
        if (!hasAvailableCapacity(quantity)) {
            throw new IllegalStateException(
                    "No hay cupo suficiente. Disponibles: " + remainingCapacity()
            );
        }
        this.soldTickets += quantity;
    }

    /**
     * Decrementa el contador de boletas vendidas.
     */
    public void decrementSoldTickets(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("La cantidad debe ser al menos 1");
        }
        if (soldTickets < quantity) {
            throw new IllegalStateException("No hay suficientes boletas para liberar");
        }
        this.soldTickets -= quantity;
    }

    // ── Getters y Setters ────────────────────────────────────────────

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public Integer getSoldTickets() { return soldTickets; }
    public void setSoldTickets(Integer soldTickets) { this.soldTickets = soldTickets; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public String getOrganizerKeycloakId() { return organizerKeycloakId; }
    public void setOrganizerKeycloakId(String organizerKeycloakId) {
        this.organizerKeycloakId = organizerKeycloakId;
    }

    public List<TicketType> getTicketTypes() { return ticketTypes; }
    public void setTicketTypes(List<TicketType> ticketTypes) { this.ticketTypes = ticketTypes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}