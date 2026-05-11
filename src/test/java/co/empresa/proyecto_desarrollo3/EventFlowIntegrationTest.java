package co.empresa.proyecto_desarrollo3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                    JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(list.size()).isEqualTo(0);
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
                    JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(list.size()).isEqualTo(1);
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

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor organizerJwt() {
        return jwt().jwt(jwt -> jwt.subject("org-1"))
                .authorities(new SimpleGrantedAuthority("ROLE_EVENT_CREATOR"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor orderServiceJwt() {
        return jwt().jwt(jwt -> jwt.subject("order-service"))
                .authorities(new SimpleGrantedAuthority("ROLE_ORDER_SERVICE"));
    }
}
