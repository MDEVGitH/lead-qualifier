package com.crm.qualifier.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Lead Record Tests")
class LeadTest {

    private static final LocalDate PAST_DATE = LocalDate.of(1990, 5, 15);

    @Test
    @DisplayName("Should create a valid Lead")
    void shouldCreateValidLead() {
        Lead lead = new Lead("123456789", PAST_DATE, "John", "Doe", "john@example.com");
        assertEquals("123456789", lead.nationalId());
        assertEquals(PAST_DATE, lead.birthdate());
        assertEquals("John", lead.firstName());
        assertEquals("Doe", lead.lastName());
        assertEquals("john@example.com", lead.email());
    }

    @Test
    @DisplayName("Should throw when nationalId is null")
    void shouldThrowWhenNationalIdIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Lead(null, PAST_DATE, "John", "Doe", "john@example.com"));
    }

    @Test
    @DisplayName("Should throw when birthdate is null")
    void shouldThrowWhenBirthdateIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Lead("123", null, "John", "Doe", "john@example.com"));
    }

    @Test
    @DisplayName("Should throw when firstName is null")
    void shouldThrowWhenFirstNameIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Lead("123", PAST_DATE, null, "Doe", "john@example.com"));
    }

    @Test
    @DisplayName("Should throw when lastName is null")
    void shouldThrowWhenLastNameIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Lead("123", PAST_DATE, "John", null, "john@example.com"));
    }

    @Test
    @DisplayName("Should throw when email is null")
    void shouldThrowWhenEmailIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Lead("123", PAST_DATE, "John", "Doe", null));
    }

    @Test
    @DisplayName("Should throw when nationalId is blank")
    void shouldThrowWhenNationalIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new Lead("  ", PAST_DATE, "John", "Doe", "john@example.com"));
    }

    @Test
    @DisplayName("Should throw when firstName is blank")
    void shouldThrowWhenFirstNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new Lead("123", PAST_DATE, "", "Doe", "john@example.com"));
    }

    @Test
    @DisplayName("Should throw when lastName is blank")
    void shouldThrowWhenLastNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new Lead("123", PAST_DATE, "John", "  ", "john@example.com"));
    }

    @Test
    @DisplayName("Should throw when email is invalid (no @)")
    void shouldThrowWhenEmailInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
            new Lead("123", PAST_DATE, "John", "Doe", "invalidemail"));
    }

    @Test
    @DisplayName("Should throw when birthdate is in the future")
    void shouldThrowWhenBirthdateIsFuture() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        assertThrows(IllegalArgumentException.class, () ->
            new Lead("123", futureDate, "John", "Doe", "john@example.com"));
    }

    @Test
    @DisplayName("Should throw when birthdate is today")
    void shouldThrowWhenBirthdateIsToday() {
        assertThrows(IllegalArgumentException.class, () ->
            new Lead("123", LocalDate.now(), "John", "Doe", "john@example.com"));
    }
}
