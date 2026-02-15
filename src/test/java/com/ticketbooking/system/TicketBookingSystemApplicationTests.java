package com.ticketbooking.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketBookingSystemApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void holdConfirmAndAvailabilityFlow() throws Exception {
        String eventBody = objectMapper.writeValueAsString(Map.of(
                "name", "Rock Fest",
                "eventDate", LocalDateTime.now().plusDays(2).toString(),
                "location", "Main Hall",
                "totalSeats", 50
        ));

        String eventResp = performPost("/api/events", eventBody)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long eventId = objectMapper.readTree(eventResp).get("id").asLong();

        String holdBody = objectMapper.writeValueAsString(Map.of(
                "userId", "user-1",
                "seatNumbers", List.of(1, 2, 3)
        ));

        String holdResp = performPost("/api/events/" + eventId + "/holds", holdBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holdId").exists())
                .andReturn().getResponse().getContentAsString();

        String holdId = objectMapper.readTree(holdResp).get("holdId").asText();

        performGet("/api/events/" + eventId + "/availability")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heldSeats").value(3))
                .andExpect(jsonPath("$.bookedSeats").value(0))
                .andExpect(jsonPath("$.availableSeats").value(47));

        String confirmBody = objectMapper.writeValueAsString(Map.of("holdId", holdId));

        String bookingResp = performPost("/api/bookings/confirm", confirmBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();

        JsonNode bookingNode = objectMapper.readTree(bookingResp);
        Long bookingId = bookingNode.get("bookingId").asLong();

        performGet("/api/bookings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(bookingId));

        performGet("/api/bookings/" + bookingId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId));

        performGet("/api/events/" + eventId + "/availability")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heldSeats").value(0))
                .andExpect(jsonPath("$.bookedSeats").value(3))
                .andExpect(jsonPath("$.availableSeats").value(47));
    }

    @Test
    void createEventsAsArrayAndFetchAllAvailability() throws Exception {
        String bulkEventBody = objectMapper.writeValueAsString(List.of(
                Map.of(
                        "name", "Tech Expo",
                        "eventDate", LocalDateTime.now().plusDays(3).toString(),
                        "location", "Hall A",
                        "totalSeats", 80
                ),
                Map.of(
                        "name", "Food Fest",
                        "eventDate", LocalDateTime.now().plusDays(4).toString(),
                        "location", "Hall B",
                        "totalSeats", 60
                )
        ));

        performPost("/api/events", bulkEventBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[1].id").exists());

        performGet("/api/events/availability")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").exists());
    }

    @Test
    void creatingSameEventTwiceIsIdempotentAndDoesNotDuplicate() throws Exception {
        String eventName = "Idempotent Event " + System.nanoTime();
        String eventDate = LocalDateTime.now().plusDays(5).withNano(0).toString();

        String eventBody = objectMapper.writeValueAsString(Map.of(
                "name", eventName,
                "eventDate", eventDate,
                "location", "Design Hall",
                "totalSeats", 120
        ));

        String firstResponse = performPost("/api/events", eventBody)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = performPost("/api/events", eventBody)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long firstId = objectMapper.readTree(firstResponse).get("id").asLong();
        Long secondId = objectMapper.readTree(secondResponse).get("id").asLong();

        Assertions.assertEquals(firstId, secondId);
    }

    @Test
    void missingCorrelationIdIsGeneratedAndReturned() throws Exception {
        String eventBody = objectMapper.writeValueAsString(Map.of(
                "name", "No Header Event",
                "eventDate", LocalDateTime.now().plusDays(2).toString(),
                "location", "Main Hall",
                "totalSeats", 50
        ));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody)
                        .header("X-Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andExpect(result -> {
                    String cid = result.getResponse().getHeader("X-Correlation-Id");
                    org.junit.jupiter.api.Assertions.assertNotNull(cid);
                    org.junit.jupiter.api.Assertions.assertFalse(cid.isBlank());
                });
    }

    @Test
    void seatAlreadyHeldReturnsConflict() throws Exception {
        String eventBody = objectMapper.writeValueAsString(Map.of(
                "name", "Conflict Event " + System.nanoTime(),
                "eventDate", LocalDateTime.now().plusDays(2).toString(),
                "location", "Main Hall",
                "totalSeats", 20
        ));

        String eventResp = performPost("/api/events", eventBody)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long eventId = objectMapper.readTree(eventResp).get("id").asLong();

        String hold1 = objectMapper.writeValueAsString(Map.of(
                "userId", "user-a",
                "seatNumbers", List.of(4)
        ));
        performPost("/api/events/" + eventId + "/holds", hold1)
                .andExpect(status().isCreated());

        String hold2 = objectMapper.writeValueAsString(Map.of(
                "userId", "user-b",
                "seatNumbers", List.of(4)
        ));
        performPost("/api/events/" + eventId + "/holds", hold2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private ResultActions performPost(String uri, String body) throws Exception {
        return mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("X-Correlation-Id", UUID.randomUUID().toString())
                .header("X-Idempotency-Key", UUID.randomUUID().toString()));
    }

    private ResultActions performGet(String uri) throws Exception {
        return mockMvc.perform(get(uri)
                .header("X-Correlation-Id", UUID.randomUUID().toString()));
    }
}
