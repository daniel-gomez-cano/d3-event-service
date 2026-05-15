package co.empresa.proyecto_desarrollo3.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class EventSearchRequest {

    // Palabra clave: busca en nombre y descripción del evento
    private String keyword;

    // Filtro por ubicación (búsqueda parcial, no exacta)
    private String location;

    // Rango de fechas
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    // Paginación
    private int page = 0;
    private int size = 20;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }

    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(0, page); }

    public int getSize() { return size; }
    public void setSize(int size) {
        // Máximo 50 resultados por página para proteger el servidor
        this.size = Math.min(size, 50);
    }
}
