package com.crm.qualifier.service;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a national registry lookup service.
 * Response distribution: ~70% MATCH, ~20% MISMATCH, ~10% NOT_FOUND.
 * Latency: 200-800ms.
 */
public class RegistryService {

    public record RegistryResponse(
        RegistryStatus status,
        String firstName,
        String lastName,
        LocalDate birthdate
    ) {}

    public enum RegistryStatus {
        MATCH, MISMATCH, NOT_FOUND
    }

    /**
     * Checks the national registry for a person's data.
     *
     * @param nationalId the person's national ID
     * @param firstName  the expected first name
     * @param lastName   the expected last name
     * @param birthdate  the expected birthdate
     * @return a RegistryResponse with the lookup result
     */
    public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
        // Simulate network latency: 200-800ms
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(200, 801));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Registry service interrupted", e);
        }

        // Simulate probabilistic outcome
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 70) {
            // ~70% MATCH — return exact input data
            return new RegistryResponse(RegistryStatus.MATCH, firstName, lastName, birthdate);
        } else if (roll < 90) {
            // ~20% MISMATCH — return different first name
            return new RegistryResponse(RegistryStatus.MISMATCH, "Unknown", lastName, birthdate);
        } else {
            // ~10% NOT_FOUND — person not in registry
            return new RegistryResponse(RegistryStatus.NOT_FOUND, null, null, null);
        }
    }
}
