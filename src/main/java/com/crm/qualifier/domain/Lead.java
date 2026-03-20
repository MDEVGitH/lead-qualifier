package com.crm.qualifier.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an unqualified potential customer entering the CRM pipeline.
 * All fields are required and validated in the compact constructor.
 */
public record Lead(
    String nationalId,
    LocalDate birthdate,
    String firstName,
    String lastName,
    String email
) {
    public Lead {
        Objects.requireNonNull(nationalId, "nationalId cannot be null");
        Objects.requireNonNull(birthdate, "birthdate cannot be null");
        Objects.requireNonNull(firstName, "firstName cannot be null");
        Objects.requireNonNull(lastName, "lastName cannot be null");
        Objects.requireNonNull(email, "email cannot be null");
        if (nationalId.isBlank()) throw new IllegalArgumentException("nationalId cannot be blank");
        if (firstName.isBlank()) throw new IllegalArgumentException("firstName cannot be blank");
        if (lastName.isBlank()) throw new IllegalArgumentException("lastName cannot be blank");
        if (!email.contains("@")) throw new IllegalArgumentException("email must be valid");
        if (!birthdate.isBefore(LocalDate.now())) throw new IllegalArgumentException("birthdate must be in the past");
    }
}
