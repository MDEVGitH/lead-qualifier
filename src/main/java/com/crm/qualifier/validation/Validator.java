package com.crm.qualifier.validation;

import com.crm.qualifier.domain.Lead;
import com.crm.qualifier.domain.ValidationResult;

/**
 * Functional interface for all validation steps in the qualification pipeline.
 */
@FunctionalInterface
public interface Validator {
    /**
     * Validates a lead against a specific criterion.
     *
     * @param lead the lead to validate
     * @return the result of the validation (never null, never throws)
     */
    ValidationResult validate(Lead lead);
}
