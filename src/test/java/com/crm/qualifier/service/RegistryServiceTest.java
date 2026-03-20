package com.crm.qualifier.service;

import com.crm.qualifier.service.RegistryService.RegistryResponse;
import com.crm.qualifier.service.RegistryService.RegistryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Registry Service Tests")
class RegistryServiceTest {

    private final RegistryService service = new RegistryService();

    @Test
    @DisplayName("Should return a valid RegistryResponse")
    void shouldReturnValidResponse() {
        RegistryResponse response = service.check("123", "John", "Doe", LocalDate.of(1990, 1, 1));
        assertNotNull(response);
        assertNotNull(response.status());
    }

    @RepeatedTest(20)
    @DisplayName("Should return responses within valid status set")
    void shouldReturnValidStatus() {
        RegistryResponse response = service.check("123", "John", "Doe", LocalDate.of(1990, 1, 1));
        assertTrue(
            response.status() == RegistryStatus.MATCH ||
            response.status() == RegistryStatus.MISMATCH ||
            response.status() == RegistryStatus.NOT_FOUND,
            "Status should be one of MATCH, MISMATCH, NOT_FOUND"
        );
    }

    @Test
    @DisplayName("Should simulate latency (response time > 0ms)")
    void shouldHaveLatency() {
        long start = System.currentTimeMillis();
        service.check("123", "John", "Doe", LocalDate.of(1990, 1, 1));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 150, "Should have at least ~200ms latency, was " + elapsed + "ms");
    }

    @Test
    @DisplayName("Should produce multiple statuses over many calls")
    void shouldProduceMultipleStatuses() {
        Map<RegistryStatus, Integer> counts = new EnumMap<>(RegistryStatus.class);
        for (int i = 0; i < 100; i++) {
            RegistryResponse response = service.check("id-" + i, "John", "Doe", LocalDate.of(1990, 1, 1));
            counts.merge(response.status(), 1, Integer::sum);
        }
        // With 100 calls, we should see at least 2 different statuses
        assertTrue(counts.size() >= 2, "Expected at least 2 different statuses over 100 calls, got: " + counts);
    }
}
