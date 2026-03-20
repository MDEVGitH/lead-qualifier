package com.crm.qualifier.validation;

import com.crm.qualifier.cache.ComplianceCache;
import com.crm.qualifier.domain.Lead;
import com.crm.qualifier.domain.ValidationResult;
import com.crm.qualifier.service.ComplianceBureauService;
import com.crm.qualifier.service.ComplianceBureauService.ComplianceResponse;
import com.crm.qualifier.service.ComplianceBureauService.ComplianceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Compliance Bureau Validator Tests")
class ComplianceBureauValidatorTest {

    private static final Lead TEST_LEAD = new Lead(
        "123456789", LocalDate.of(1990, 5, 15), "John", "Doe", "john@example.com"
    );

    @TempDir
    Path tempDir;
    private ComplianceCache cache;

    @BeforeEach
    void setUp() {
        Path cacheFile = tempDir.resolve("cache.json");
        cache = new ComplianceCache(cacheFile, Duration.ofHours(24));
    }

    @Test
    @DisplayName("Should pass when service returns CLEAR")
    void shouldPassWhenClear() {
        ComplianceBureauService mockService = new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                return new ComplianceResponse(ComplianceStatus.CLEAR);
            }
        };

        ComplianceBureauValidator validator = new ComplianceBureauValidator(mockService, cache);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertTrue(result.success());
        assertTrue(result.message().contains("CLEAR"));
    }

    @Test
    @DisplayName("Should fail when service returns FLAGGED")
    void shouldFailWhenFlagged() {
        ComplianceBureauService mockService = new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                return new ComplianceResponse(ComplianceStatus.FLAGGED);
            }
        };

        ComplianceBureauValidator validator = new ComplianceBureauValidator(mockService, cache);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertFalse(result.success());
        assertTrue(result.message().contains("FLAGGED"));
    }

    @Test
    @DisplayName("Should return manual review when service is down")
    void shouldReturnManualReviewWhenServiceDown() {
        ComplianceBureauService mockService = new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                throw new RuntimeException("Compliance Bureau service unavailable");
            }
        };

        ComplianceBureauValidator validator = new ComplianceBureauValidator(mockService, cache);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertFalse(result.success());
        assertTrue(result.message().contains("SERVICE_UNAVAILABLE"));
        assertTrue(result.message().contains("Manual review required"));
    }

    @Test
    @DisplayName("Should use cached result on cache hit")
    void shouldUseCachedResult() {
        // Pre-populate cache
        cache.put(TEST_LEAD.nationalId(), ComplianceStatus.CLEAR);

        // Service that would fail if called
        ComplianceBureauService failService = new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                fail("Service should not be called when cache has a hit");
                return null;
            }
        };

        ComplianceBureauValidator validator = new ComplianceBureauValidator(failService, cache);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertTrue(result.success());
        assertTrue(result.message().contains("CLEAR"));
        assertTrue(result.message().contains("cached"));
    }

    @Test
    @DisplayName("Should call service on cache miss")
    void shouldCallServiceOnCacheMiss() {
        ComplianceBureauService mockService = new ComplianceBureauService() {
            @Override
            public ComplianceResponse check(String nationalId) {
                return new ComplianceResponse(ComplianceStatus.CLEAR);
            }
        };

        ComplianceBureauValidator validator = new ComplianceBureauValidator(mockService, cache);
        ValidationResult result = validator.validate(TEST_LEAD);

        assertTrue(result.success());
        // After call, result should be cached
        assertTrue(cache.get(TEST_LEAD.nationalId()).isPresent());
    }
}
