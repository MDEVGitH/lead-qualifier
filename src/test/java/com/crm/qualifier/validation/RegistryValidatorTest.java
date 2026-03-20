package com.crm.qualifier.validation;

import com.crm.qualifier.domain.Lead;
import com.crm.qualifier.domain.ValidationResult;
import com.crm.qualifier.service.RegistryService;
import com.crm.qualifier.service.RegistryService.RegistryResponse;
import com.crm.qualifier.service.RegistryService.RegistryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Registry Validator Tests")
class RegistryValidatorTest {

    private static final Lead TEST_LEAD = new Lead(
        "123456789", LocalDate.of(1990, 5, 15), "John", "Doe", "john@example.com"
    );

    @Test
    @DisplayName("Should pass when registry returns MATCH")
    void shouldPassOnMatch() {
        // Create a service that always returns MATCH
        RegistryService mockService = new RegistryService() {
            @Override
            public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
                return new RegistryResponse(RegistryStatus.MATCH, firstName, lastName, birthdate);
            }
        };

        RegistryValidator validator = new RegistryValidator(mockService);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertTrue(result.success());
        assertEquals("RegistryValidator", result.validatorName());
        assertTrue(result.message().contains("MATCH"));
    }

    @Test
    @DisplayName("Should fail when registry returns MISMATCH")
    void shouldFailOnMismatch() {
        RegistryService mockService = new RegistryService() {
            @Override
            public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
                return new RegistryResponse(RegistryStatus.MISMATCH, "Unknown", lastName, birthdate);
            }
        };

        RegistryValidator validator = new RegistryValidator(mockService);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertFalse(result.success());
        assertTrue(result.message().contains("MISMATCH"));
    }

    @Test
    @DisplayName("Should fail when registry returns NOT_FOUND")
    void shouldFailOnNotFound() {
        RegistryService mockService = new RegistryService() {
            @Override
            public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
                return new RegistryResponse(RegistryStatus.NOT_FOUND, null, null, null);
            }
        };

        RegistryValidator validator = new RegistryValidator(mockService);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertFalse(result.success());
        assertTrue(result.message().contains("NOT_FOUND"));
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void shouldHandleServiceException() {
        RegistryService mockService = new RegistryService() {
            @Override
            public RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate) {
                throw new RuntimeException("Connection timeout");
            }
        };

        RegistryValidator validator = new RegistryValidator(mockService);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertFalse(result.success());
        assertTrue(result.message().contains("ERROR"));
    }
}
