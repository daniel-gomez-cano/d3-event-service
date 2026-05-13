package co.empresa.proyecto_desarrollo3.dto.response;

import co.empresa.proyecto_desarrollo3.model.Event;
import co.empresa.proyecto_desarrollo3.model.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EventListItemResponse {

    private Long id;
    private String name;
    private LocalDateTime eventDate;
    private String location;
    private String imageUrl;
    private BigDecimal minPrice;
    private boolean soldOut;

    public static EventListItemResponse fromEntity(Event event) {
        EventListItemResponse dto = new EventListItemResponse();
        dto.id = event.getId();
        dto.name = event.getName();
        dto.eventDate = event.getEventDate();
        dto.location = event.getLocation();
        dto.imageUrl = event.getImageUrl();

        List<TicketType> activeTypes = event.getTicketTypes().stream()
                .filter(TicketType::getActive)
                .collect(Collectors.toList());

        dto.minPrice = activeTypes.stream()
                .map(TicketType::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);

        dto.soldOut = !activeTypes.isEmpty()
                && activeTypes.stream().allMatch(tt -> tt.remainingStock() <= 0);

        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getEventDate() { return eventDate; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getMinPrice() { return minPrice; }
    public boolean isSoldOut() { return soldOut; }
}
