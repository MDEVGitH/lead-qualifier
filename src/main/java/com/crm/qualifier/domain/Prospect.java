package com.crm.qualifier.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a qualified lead that has passed all validation steps.
 * Contains all Lead fields plus qualification metadata.
 */
public record Prospect(
    String nationalId,
    LocalDate birthdate,
    String firstName,
    String lastName,
    String email,
    int qualificationScore,
    LocalDateTime qualifiedAt
) {
    public Prospect {
        Objects.requireNonNull(nationalId, "nationalId cannot be null");
        Objects.requireNonNull(birthdate, "birthdate cannot be null");
        Objects.requireNonNull(firstName, "firstName cannot be null");
        Objects.requireNonNull(lastName, "lastName cannot be null");
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(qualifiedAt, "qualifiedAt cannot be null");
        if (qualificationScore < 0 || qualificationScore > 100) {
            throw new IllegalArgumentException("qualificationScore must be between 0 and 100");
        }
    }

    /**
     * Creates a Prospect from an existing Lead with qualification data.
     */
    public static Prospect fromLead(Lead lead, int qualificationScore, LocalDateTime qualifiedAt) {
        return new Prospect(
            lead.nationalId(),
            lead.birthdate(),
            lead.firstName(),
            lead.lastName(),
            lead.email(),
            qualificationScore,
            qualifiedAt
        );
    }
}
