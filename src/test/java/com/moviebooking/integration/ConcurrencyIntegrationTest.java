package com.moviebooking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Concurrency test: 10 threads race to book the SAME single seat.
 * Exactly 1 should succeed; the rest should get 409 Conflict.
 */
class ConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Test
    void onlyOneThreadSucceedsInHoldingSameSeat() throws Exception {
        // Admin setup
        MvcResult cityR = mockMvc.perform(post("/api/admin/cities")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "ConcCity_" + System.currentTimeMillis()))))
                .andReturn();
        long cityId = id(cityR, "data.id");

        MvcResult theaterR = mockMvc.perform(post("/api/admin/theaters")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("cityId", cityId, "name", "Theater"))))
                .andReturn();
        long theaterId = id(theaterR, "data.id");

        mockMvc.perform(post("/api/admin/theaters/" + theaterId + "/screens")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "S1", "totalRows", 1, "seatsPerRow", 1, "premiumRows", 0))))
                .andReturn();

        MvcResult movieR = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "RaceMovie", "durationMins", 120))))
                .andReturn();
        long movieId = id(movieR, "data.id");

        // Find the screen ID
        JsonNode screenNodes = objectMapper.readTree(
                mockMvc.perform(get("/api/admin/theaters").param("cityId", String.valueOf(cityId))
                        .header("Authorization", authHeader(adminToken))).andReturn()
                        .getResponse().getContentAsString()).path("data");
        // Use first theater's first screen — we'll grab it from the db directly via seat map
        // Find shows — create one first
        LocalDateTime start = LocalDateTime.now().plusDays(7);
        MvcResult screensList = mockMvc.perform(get("/api/admin/theaters")
                .header("Authorization", authHeader(adminToken))
                .param("cityId", String.valueOf(cityId))).andReturn();

        // Grab screenId from DB — find show
        MvcResult showR = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "screenId", id(
                                        mockMvc.perform(post("/api/admin/theaters/" + theaterId + "/screens")
                                                .header("Authorization", authHeader(adminToken))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(json(Map.of("name", "S2", "totalRows", 1, "seatsPerRow", 1, "premiumRows", 0))))
                                                .andReturn(), "data.id"),
                                "movieId", movieId,
                                "startTime", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                "endTime", start.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        ))))
                .andReturn();
        long showId = id(showR, "data.id");

        // Get the single seat id
        MvcResult seatMapR = mockMvc.perform(get("/api/shows/" + showId + "/seats")).andReturn();
        long showSeatId = objectMapper.readTree(seatMapR.getResponse().getContentAsString())
                .path("data").path("seats").get(0).path("showSeatId").asLong();

        // Create 10 customer tokens
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String email = "racer" + i + "_" + System.currentTimeMillis() + "@test.com";
            register(email, "Racer1234!", "Racer " + i);
            tokens.add(login(email, "Racer1234!"));
        }

        // Race — all 10 customers try to hold the same seat simultaneously
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Future<Integer>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        for (String token : tokens) {
            futures.add(pool.submit(() -> {
                latch.await(); // start all at once
                try {
                    return mockMvc.perform(post("/api/bookings")
                                    .header("Authorization", authHeader(token))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(Map.of("showId", showId, "showSeatIds", List.of(showSeatId)))))
                            .andReturn().getResponse().getStatus();
                } catch (Exception e) {
                    return 500;
                }
            }));
        }

        latch.countDown(); // fire!
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        for (Future<Integer> f : futures) {
            int status = f.get();
            if (status == 201) successes.incrementAndGet();
            else if (status == 409) conflicts.incrementAndGet();
        }

        assertThat(successes.get())
                .as("Exactly one thread should succeed in holding the seat")
                .isEqualTo(1);
        assertThat(conflicts.get())
                .as("All other threads should get 409 Conflict")
                .isEqualTo(9);
    }

    private long id(MvcResult r, String path) throws Exception {
        String[] parts = path.split("\\.");
        JsonNode node = objectMapper.readTree(r.getResponse().getContentAsString());
        for (String p : parts) node = node.path(p);
        return node.asLong();
    }
}
