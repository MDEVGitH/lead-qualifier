package com.crm.qualifier.domain;

/**
 * Represents the final outcome of the qualification pipeline.
 */
public enum QualificationStatus {
    /** All validations passed, score > 60. Lead becomes Prospect. */
    APPROVED,
    /** One or more validations failed. Lead is not qualified. */
    REJECTED,
    /** Compliance service was unavailable. Requires human review. */
    MANUAL_REVIEW
}
