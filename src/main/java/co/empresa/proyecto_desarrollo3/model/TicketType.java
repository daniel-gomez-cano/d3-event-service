package co.empresa.proyecto_desarrollo3.model;


import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ticket_types")
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @Column(nullable = false)
    private Boolean active = true;

    public TicketType() {}

    public TicketType(Event event, String name, BigDecimal price, Integer availableQuantity) {
        this.event = event;
        this.name = name;
        this.price = price;
        this.availableQuantity = availableQuantity;
        this.soldQuantity = 0;
        this.active = true;
    }

    /**
     * Verifica si hay stock disponible para este tipo de boleta.
     */
    public boolean hasStock(int quantity) {
        return (soldQuantity + quantity) <= availableQuantity;
    }

    public int remainingStock() {
        return availableQuantity - soldQuantity;
    }

    public Long getId() { return id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}