package com.crm.qualifier.service;

import com.crm.qualifier.service.ComplianceBureauService.ComplianceResponse;
import com.crm.qualifier.service.ComplianceBureauService.ComplianceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Compliance Bureau Service Tests")
class ComplianceBureauServiceTest {

    private final ComplianceBureauService service = new ComplianceBureauService();

    @Test
    @DisplayName("Should return valid responses or throw RuntimeException")
    void shouldReturnValidResponsesOrThrow() {
        int successCount = 0;
        int exceptionCount = 0;

        for (int i = 0; i < 100; i++) {
            try {
                ComplianceResponse response = service.check("id-" + i);
                assertNotNull(response);
                assertNotNull(response.status());
                assertTrue(
                    response.status() == ComplianceStatus.CLEAR ||
                    response.status() == ComplianceStatus.FLAGGED
                );
                successCount++;
            } catch (RuntimeException e) {
                assertEquals("Compliance Bureau service unavailable", e.getMessage());
                exceptionCount++;
            }
        }

        // Should have some successful calls and some exceptions over 100 calls
        assertTrue(successCount > 0, "Expected at least some successful responses");
        // With ~10% exception rate, over 100 calls we should see at least 1
        assertTrue(exceptionCount > 0, "Expected at least some exceptions over 100 calls, got: " + exceptionCount);
    }

    @Test
    @DisplayName("Should have latency")
    void shouldHaveLatency() {
        long start = System.currentTimeMillis();
        try {
            service.check("test-id");
        } catch (RuntimeException e) {
            // Expected sometimes
        }
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 80, "Should have at least ~100ms latency, was " + elapsed + "ms");
    }
}
