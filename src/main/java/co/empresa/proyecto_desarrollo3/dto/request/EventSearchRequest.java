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

    // Paginacion (1-based)
    private int page = 1;
    private int limit = 12;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }

    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
