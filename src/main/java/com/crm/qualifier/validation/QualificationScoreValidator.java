package com.crm.qualifier.validation;

import com.crm.qualifier.domain.Lead;
import com.crm.qualifier.domain.ValidationResult;
import com.crm.qualifier.service.QualificationScoreService;

import java.time.LocalDateTime;

/**
 * Validates a lead by generating a random qualification score.
 * Score must be strictly greater than 60 to pass.
 */
public class QualificationScoreValidator implements Validator {

    private static final String VALIDATOR_NAME = "QualificationScoreValidator";
    private static final int THRESHOLD = 60;
    private final QualificationScoreService scoreService;

    private int lastScore = -1;

    public QualificationScoreValidator(QualificationScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @Override
    public ValidationResult validate(Lead lead) {
        System.out.println("[SCORE] Generating qualification score for: " + lead.nationalId());

        int score = scoreService.generateScore();
        this.lastScore = score;
        boolean passed = score > THRESHOLD;

        if (passed) {
            System.out.println("[SCORE] Score: " + score + "/100 - PASSED (>" + THRESHOLD + ")");
            return new ValidationResult(true, VALIDATOR_NAME,
                "Score: " + score + "/100 - Above threshold (>" + THRESHOLD + ")", LocalDateTime.now());
        } else {
            System.out.println("[SCORE] Score: " + score + "/100 - FAILED (<=" + THRESHOLD + ")");
            return new ValidationResult(false, VALIDATOR_NAME,
                "Score: " + score + "/100 - Below threshold (>" + THRESHOLD + ")", LocalDateTime.now());
        }
    }

    /**
     * Returns the last generated score, useful for building Prospect.
     * Returns -1 if no score has been generated yet.
     */
    public int getLastScore() {
        return lastScore;
    }
}
