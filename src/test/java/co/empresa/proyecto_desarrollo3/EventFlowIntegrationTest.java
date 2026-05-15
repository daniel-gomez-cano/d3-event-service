package co.empresa.proyecto_desarrollo3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.empresa.proyecto_desarrollo3.model.Event;
import co.empresa.proyecto_desarrollo3.model.TicketType;
import co.empresa.proyecto_desarrollo3.model.enums.EventStatus;
import co.empresa.proyecto_desarrollo3.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void cleanDb() {
        eventRepository.deleteAll();
    }

    @Test
    void createDraftPublishReserveReleaseFlow() throws Exception {
        String createPayload = createEventPayload("Evento Test", 10, 10);

        MvcResult created = mockMvc.perform(post("/api/v1/events")
                        .with(organizerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        Long eventId = createdJson.get("id").asLong();
        Long ticketTypeId = createdJson.get("ticketTypes").get(0).get("id").asLong();
        assertThat(createdJson.get("status").asText()).isEqualTo("DRAFT");

                mockMvc.perform(get("/api/v1/events"))
                                .andExpect(status().isOk())
                                .andExpect(result -> {
                                        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
                                        assertThat(response.get("content").size()).isEqualTo(0);
                                });

        mockMvc.perform(patch("/api/v1/events/{id}/publish", eventId)
                        .with(organizerJwt()))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(json.get("status").asText()).isEqualTo("PUBLISHED");
                });

                mockMvc.perform(get("/api/v1/events"))
                                .andExpect(status().isOk())
                                .andExpect(result -> {
                                        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
                                        assertThat(response.get("content").size()).isEqualTo(1);
                                });

        String reservePayload = objectMapper.writeValueAsString(Map.of(
                "ticketTypeId", ticketTypeId,
                "quantity", 3
        ));

        mockMvc.perform(post("/api/v1/events/{id}/reserve", eventId)
                        .with(orderServiceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservePayload))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(json.get("soldTickets").asInt()).isEqualTo(3);
                });

        String releasePayload = objectMapper.writeValueAsString(Map.of(
                "ticketTypeId", ticketTypeId,
                "quantity", 2
        ));

        mockMvc.perform(post("/api/v1/events/{id}/release", eventId)
                        .with(orderServiceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(releasePayload))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(json.get("soldTickets").asInt()).isEqualTo(1);
                });
    }

    @Test
    void reserveOverCapacityReturnsConflict() throws Exception {
        String createPayload = createEventPayload("Evento Cupos", 5, 5);

        MvcResult created = mockMvc.perform(post("/api/v1/events")
                        .with(organizerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        Long eventId = createdJson.get("id").asLong();
        Long ticketTypeId = createdJson.get("ticketTypes").get(0).get("id").asLong();

        mockMvc.perform(patch("/api/v1/events/{id}/publish", eventId)
                        .with(organizerJwt()))
                .andExpect(status().isOk());

        String reservePayload = objectMapper.writeValueAsString(Map.of(
                "ticketTypeId", ticketTypeId,
                "quantity", 6
        ));

        mockMvc.perform(post("/api/v1/events/{id}/reserve", eventId)
                        .with(orderServiceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservePayload))
                .andExpect(status().isConflict());
    }

    @Test
    void listingDefaultsIncludeMinPriceAndSoldOut() throws Exception {
        String createPayload = createEventPayloadWithTypes(
                "Evento Precios",
                6,
                List.of(
                        Map.of("name", "General", "price", 30.0, "quantity", 3),
                        Map.of("name", "VIP", "price", 50.0, "quantity", 3)
                )
        );

        MvcResult created = mockMvc.perform(post("/api/v1/events")
                        .with(organizerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        Long eventId = createdJson.get("id").asLong();

        mockMvc.perform(patch("/api/v1/events/{id}/publish", eventId)
                        .with(organizerJwt()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
                    JsonNode first = response.get("content").get(0);
                    assertThat(response.get("page").asInt()).isEqualTo(1);
                    assertThat(response.get("size").asInt()).isEqualTo(12);
                    assertThat(first.get("minPrice").asDouble()).isEqualTo(30.0);
                    assertThat(first.get("soldOut").asBoolean()).isFalse();
                                        assertThat(first.get("ticketTypes")).isNull();
                });
    }

    @Test
    void listingShowsSoldOutWhenAllTypesEmpty() throws Exception {
        String createPayload = createEventPayloadWithTypes(
                "Evento SoldOut",
                2,
                List.of(
                        Map.of("name", "General", "price", 20.0, "quantity", 1),
                        Map.of("name", "VIP", "price", 40.0, "quantity", 1)
                )
        );

        MvcResult created = mockMvc.perform(post("/api/v1/events")
                        .with(organizerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        Long eventId = createdJson.get("id").asLong();
        Long ticketTypeIdA = createdJson.get("ticketTypes").get(0).get("id").asLong();
        Long ticketTypeIdB = createdJson.get("ticketTypes").get(1).get("id").asLong();

        mockMvc.perform(patch("/api/v1/events/{id}/publish", eventId)
                        .with(organizerJwt()))
                .andExpect(status().isOk());

        String reservePayloadA = objectMapper.writeValueAsString(Map.of(
                "ticketTypeId", ticketTypeIdA,
                "quantity", 1
        ));

        String reservePayloadB = objectMapper.writeValueAsString(Map.of(
                "ticketTypeId", ticketTypeIdB,
                "quantity", 1
        ));

        mockMvc.perform(post("/api/v1/events/{id}/reserve", eventId)
                        .with(orderServiceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservePayloadA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/{id}/reserve", eventId)
                        .with(orderServiceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservePayloadB))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
                    JsonNode first = response.get("content").get(0);
                    assertThat(first.get("soldOut").asBoolean()).isTrue();
                });
    }

    @Test
    void listingReturnsPastPublishedWhenNoDateFilters() throws Exception {
        Event pastEvent = new Event();
        pastEvent.setName("Evento Pasado");
        pastEvent.setDescription("Evento pasado publicado");
        pastEvent.setEventDate(LocalDateTime.now().minusDays(2));
        pastEvent.setLocation("Medellin");
        pastEvent.setMaxCapacity(10);
        pastEvent.setOrganizerKeycloakId("org-1");
        pastEvent.setStatus(EventStatus.PUBLISHED);

        TicketType type = new TicketType(pastEvent, "General", BigDecimal.valueOf(20.0), 10);
        pastEvent.getTicketTypes().add(type);

        eventRepository.save(pastEvent);

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(response.get("content").size()).isEqualTo(1);
                    assertThat(response.get("content").get(0).get("name").asText())
                            .isEqualTo("Evento Pasado");
                });
    }

    @Test
    void searchAliasReturnsPagedResults() throws Exception {
        String createPayload = createEventPayload("Evento Alias", 5, 5);

        MvcResult created = mockMvc.perform(post("/api/v1/events")
                        .with(organizerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        Long eventId = createdJson.get("id").asLong();

        mockMvc.perform(patch("/api/v1/events/{id}/publish", eventId)
                        .with(organizerJwt()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/events/search"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(response.get("content").size()).isEqualTo(1);
                });
    }

    @Test
    void invalidPaginationReturnsUnprocessableEntity() throws Exception {
        mockMvc.perform(get("/api/v1/events?page=0"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/v1/events?limit=0"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/v1/events?limit=51"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invalidDateRangeReturnsUnprocessableEntity() throws Exception {
        mockMvc.perform(get("/api/v1/events?dateFrom=2026-06-10&dateTo=2026-06-01"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(result -> {
                    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(response.get("message").asText())
                            .isEqualTo("dateFrom must be before dateTo");
                });
    }

    private String createEventPayload(String name, int maxCapacity, int ticketQuantity) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", "Descripcion de prueba");
        payload.put("eventDate", LocalDateTime.now().plusDays(5).withNano(0));
        payload.put("location", "Bogota");
        payload.put("maxCapacity", maxCapacity);
        payload.put("ticketTypes", List.of(
                Map.of(
                        "name", "General",
                        "price", 50.0,
                        "quantity", ticketQuantity
                )
        ));
        return objectMapper.writeValueAsString(payload);
    }

        private String createEventPayloadWithTypes(
                        String name,
                        int maxCapacity,
                        List<Map<String, Object>> ticketTypes) throws Exception {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("name", name);
                payload.put("description", "Descripcion de prueba");
                payload.put("eventDate", LocalDateTime.now().plusDays(5).withNano(0));
                payload.put("location", "Bogota");
                payload.put("maxCapacity", maxCapacity);
                payload.put("ticketTypes", ticketTypes);
                return objectMapper.writeValueAsString(payload);
        }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor organizerJwt() {
        return jwt().jwt(jwt -> jwt.subject("org-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor orderServiceJwt() {
        return jwt().jwt(jwt -> jwt.subject("order-service"))
                .authorities(new SimpleGrantedAuthority("ROLE_ORDER_SERVICE"));
    }
}
