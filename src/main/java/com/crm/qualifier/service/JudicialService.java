package com.crm.qualifier.service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a national judicial archives lookup service.
 * Response distribution: ~85% CLEAN, ~15% HAS_RECORDS.
 * Latency: 300-1000ms.
 */
public class JudicialService {

    public record JudicialResponse(
        JudicialStatus status
    ) {}

    public enum JudicialStatus {
        CLEAN, HAS_RECORDS
    }

    /**
     * Checks the judicial archives for a person's records.
     *
     * @param nationalId the person's national ID
     * @return a JudicialResponse with the lookup result
     */
    public JudicialResponse check(String nationalId) {
        // Simulate network latency: 300-1000ms
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(300, 1001));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Judicial service interrupted", e);
        }

        // Simulate probabilistic outcome
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 85) {
            return new JudicialResponse(JudicialStatus.CLEAN);
        } else {
            return new JudicialResponse(JudicialStatus.HAS_RECORDS);
        }
    }
}
