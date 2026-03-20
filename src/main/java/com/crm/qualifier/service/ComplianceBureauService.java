package com.crm.qualifier.service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates an OFAC/sanctions compliance checking service.
 * Response distribution: ~80% CLEAR, ~10% FLAGGED, ~10% throws RuntimeException.
 * Latency: 100-500ms.
 */
public class ComplianceBureauService {

    public record ComplianceResponse(
        ComplianceStatus status
    ) {}

    public enum ComplianceStatus {
        CLEAR, FLAGGED
    }

    /**
     * Checks a person against OFAC/sanctions lists.
     * May throw RuntimeException to simulate service unavailability.
     *
     * @param nationalId the person's national ID
     * @return a ComplianceResponse with the check result
     * @throws RuntimeException if the service is unavailable (~10% of calls)
     */
    public ComplianceResponse check(String nationalId) {
        // Simulate network latency: 100-500ms
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(100, 501));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Compliance service interrupted", e);
        }

        // Simulate probabilistic outcome
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 80) {
            return new ComplianceResponse(ComplianceStatus.CLEAR);
        } else if (roll < 90) {
            return new ComplianceResponse(ComplianceStatus.FLAGGED);
        } else {
            throw new RuntimeException("Compliance Bureau service unavailable");
        }
    }
}
