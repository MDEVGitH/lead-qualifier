package com.crm.qualifier.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents the outcome of a single validation step in the pipeline.
 */
public record ValidationResult(
    boolean success,
    String validatorName,
    String message,
    LocalDateTime timestamp
) {
    public ValidationResult {
        Objects.requireNonNull(validatorName, "validatorName cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }
}
