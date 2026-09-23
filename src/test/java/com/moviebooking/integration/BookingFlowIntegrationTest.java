package com.moviebooking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test: admin setup → customer browses → holds → confirms → cancels.
 */
class BookingFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void fullBookingFlow() throws Exception {
        // 1. Admin creates city
        MvcResult cityResult = mockMvc.perform(post("/api/admin/cities")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Bangalore_" + System.currentTimeMillis(), "state", "KA"))))
                .andExpect(status().isCreated())
                .andReturn();
        long cityId = extractId(cityResult, "data.id");

        // 2. Admin creates theater
        MvcResult theaterResult = mockMvc.perform(post("/api/admin/theaters")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("cityId", cityId, "name", "PVR Forum", "address", "Forum Mall"))))
                .andExpect(status().isCreated())
                .andReturn();
        long theaterId = extractId(theaterResult, "data.id");

        // 3. Admin creates screen with 3 rows × 5 seats (row A = PREMIUM, rows B-C = REGULAR)
        MvcResult screenResult = mockMvc.perform(post("/api/admin/theaters/" + theaterId + "/screens")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Screen 1", "totalRows", 3, "seatsPerRow", 5, "premiumRows", 1))))
                .andExpect(status().isCreated())
                .andReturn();
        long screenId = extractId(screenResult, "data.id");

        // 4. Admin creates movie
        MvcResult movieResult = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Interstellar", "durationMins", 169, "genre", "Sci-Fi", "language", "English"))))
                .andExpect(status().isCreated())
                .andReturn();
        long movieId = extractId(movieResult, "data.id");

        // 5. Admin creates show
        LocalDateTime startTime = LocalDateTime.now().plusDays(5);
        LocalDateTime endTime = startTime.plusHours(3);
        MvcResult showResult = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "screenId", screenId,
                                "movieId", movieId,
                                "startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                "endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))))
                .andExpect(status().isCreated())
                .andReturn();
        long showId = extractId(showResult, "data.id");

        // 6. Admin sets pricing
        mockMvc.perform(post("/api/admin/shows/" + showId + "/pricing")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tiers", List.of(
                                Map.of("seatType", "REGULAR", "basePrice", 200, "weekendPricingEnabled", false, "weekendMultiplier", 1.0),
                                Map.of("seatType", "PREMIUM", "basePrice", 400, "weekendPricingEnabled", false, "weekendMultiplier", 1.0)
                        )))))
                .andExpect(status().isOk());

        // 7. Customer views seat map
        MvcResult seatMapResult = mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableSeats").value(15))
                .andReturn();

        // Pick 2 regular seats
        JsonNode seats = objectMapper.readTree(seatMapResult.getResponse().getContentAsString())
                .path("data").path("seats");
        long seat1Id = -1, seat2Id = -1;
        for (JsonNode s : seats) {
            if ("REGULAR".equals(s.path("seatType").asText())) {
                if (seat1Id < 0) seat1Id = s.path("showSeatId").asLong();
                else if (seat2Id < 0) { seat2Id = s.path("showSeatId").asLong(); break; }
            }
        }

        // 8. Customer holds seats
        MvcResult holdResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", authHeader(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showId", showId, "showSeatIds", List.of(seat1Id, seat2Id)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.holdExpiresAt").isNotEmpty())
                .andReturn();
        long bookingId = extractId(holdResult, "data.bookingId");

        // 9. Customer confirms booking
        mockMvc.perform(post("/api/bookings/" + bookingId + "/confirm")
                        .header("Authorization", authHeader(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.totalAmount").value(400.0));

        // 10. Verify seats are now BOOKED in seat map
        mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableSeats").value(13));

        // 11. Customer cancels the booking
        mockMvc.perform(delete("/api/bookings/" + bookingId)
                        .header("Authorization", authHeader(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // 12. Seats should be available again
        mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableSeats").value(15));
    }

    private long extractId(MvcResult result, String jsonPath) throws Exception {
        String[] parts = jsonPath.split("\\.");
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String part : parts) node = node.path(part);
        return node.asLong();
    }
}
