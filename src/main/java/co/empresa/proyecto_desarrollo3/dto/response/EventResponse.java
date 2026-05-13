package co.empresa.proyecto_desarrollo3.dto.response;


import co.empresa.proyecto_desarrollo3.model.Event;
import co.empresa.proyecto_desarrollo3.model.TicketType;
import co.empresa.proyecto_desarrollo3.model.enums.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EventResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private String imageUrl;
    private Integer maxCapacity;
    private Integer soldTickets;
    private Integer remainingCapacity;
    private BigDecimal minPrice;
    private boolean soldOut;
    private EventStatus status;
    private List<TicketTypeResponse> ticketTypes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponse fromEntity(Event event) {
        EventResponse dto = new EventResponse();
        dto.id = event.getId();
        dto.name = event.getName();
        dto.description = event.getDescription();
        dto.eventDate = event.getEventDate();
        dto.location = event.getLocation();
        dto.imageUrl = event.getImageUrl();
        dto.maxCapacity = event.getMaxCapacity();
        dto.soldTickets = event.getSoldTickets();
        dto.remainingCapacity = event.remainingCapacity();
        dto.status = event.getStatus();
        dto.createdAt = event.getCreatedAt();
        dto.updatedAt = event.getUpdatedAt();
        List<TicketType> activeTypes = event.getTicketTypes().stream()
            .filter(TicketType::getActive)
            .collect(Collectors.toList());

        dto.ticketTypes = activeTypes.stream()
            .map(TicketTypeResponse::fromEntity)
            .collect(Collectors.toList());

        dto.minPrice = activeTypes.stream()
            .map(TicketType::getPrice)
            .min(Comparator.naturalOrder())
            .orElse(null);

        dto.soldOut = !activeTypes.isEmpty()
            && activeTypes.stream().allMatch(tt -> tt.remainingStock() <= 0);
        return dto;
    }

    // ── Clase interna para tipos de boleta ────────────────────────────

    public static class TicketTypeResponse {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer availableQuantity;
        private Integer remainingStock;

        public static TicketTypeResponse fromEntity(TicketType tt) {
            TicketTypeResponse r = new TicketTypeResponse();
            r.id = tt.getId();
            r.name = tt.getName();
            r.price = tt.getPrice();
            r.availableQuantity = tt.getAvailableQuantity();
            r.remainingStock = tt.remainingStock();
            return r;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
        public Integer getAvailableQuantity() { return availableQuantity; }
        public Integer getRemainingStock() { return remainingStock; }
    }

    // ── Getters ───────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getEventDate() { return eventDate; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public Integer getSoldTickets() { return soldTickets; }
    public Integer getRemainingCapacity() { return remainingCapacity; }
    public BigDecimal getMinPrice() { return minPrice; }
    public boolean isSoldOut() { return soldOut; }
    public EventStatus getStatus() { return status; }
    public List<TicketTypeResponse> getTicketTypes() { return ticketTypes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}