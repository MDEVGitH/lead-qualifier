package com.crm.qualifier.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the complete result of running the qualification pipeline on a Lead.
 */
public record PipelineResult(
    List<ValidationResult> validationResults,
    QualificationStatus status,
    Prospect prospect
) {
    public PipelineResult {
        Objects.requireNonNull(validationResults, "validationResults cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        validationResults = List.copyOf(validationResults);
    }

    /**
     * Returns the prospect as an Optional.
     * Present only when status is APPROVED.
     */
    public Optional<Prospect> getProspect() {
        return Optional.ofNullable(prospect);
    }
}
