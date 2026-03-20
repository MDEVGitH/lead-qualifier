package com.crm.qualifier.service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates a random qualification score for a lead.
 * Returns a random integer in range [0, 100] inclusive.
 * No latency simulation (internal scoring function).
 */
public class QualificationScoreService {

    /**
     * Generates a random qualification score.
     *
     * @return a random score between 0 and 100 inclusive
     */
    public int generateScore() {
        return ThreadLocalRandom.current().nextInt(101);
    }
}
