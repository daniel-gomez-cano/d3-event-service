package co.empresa.proyecto_desarrollo3.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CreateEventRequest {

    @NotBlank(message = "El nombre del evento es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String name;

    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    private String description;

    @NotNull(message = "La fecha del evento es obligatoria")
    @Future(message = "La fecha del evento debe ser en el futuro")
    private LocalDateTime eventDate;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 300, message = "La ubicación no puede superar los 300 caracteres")
    private String location;

    // Opcional — URL de imagen ya subida a almacenamiento externo
    private String imageUrl;

    @NotNull(message = "El cupo máximo es obligatorio")
    @Min(value = 1, message = "El cupo máximo debe ser al menos 1")
    @Max(value = 100000, message = "El cupo máximo no puede superar 100.000")
    private Integer maxCapacity;

    @NotEmpty(message = "Debe incluir al menos un tipo de boleta")
    @Valid
    private List<TicketTypeRequest> ticketTypes;

    @AssertTrue(message = "La suma de cantidades de boletas no puede superar el cupo maximo")
    public boolean isTicketTypesWithinCapacity() {
        if (ticketTypes == null || ticketTypes.isEmpty() || maxCapacity == null) {
            return true;
        }
        int total = ticketTypes.stream()
                .mapToInt(TicketTypeRequest::getQuantity)
                .sum();
        return total <= maxCapacity;
    }

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

    public List<TicketTypeRequest> getTicketTypes() { return ticketTypes; }
    public void setTicketTypes(List<TicketTypeRequest> ticketTypes) {
        this.ticketTypes = ticketTypes;
    }

    public static class TicketTypeRequest {

        @NotBlank(message = "El nombre del tipo de boleta es obligatorio")
        private String name;

        @NotNull(message = "El precio del tipo de boleta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        private BigDecimal price;

        @NotNull(message = "La cantidad del tipo de boleta es obligatoria")
        @Min(value = 1, message = "La cantidad del tipo de boleta debe ser al menos 1")
        private Integer quantity;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
