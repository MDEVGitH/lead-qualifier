package com.crm.qualifier.validation;

import com.crm.qualifier.cache.ComplianceCache;
import com.crm.qualifier.domain.Lead;
import com.crm.qualifier.domain.ValidationResult;
import com.crm.qualifier.service.ComplianceBureauService;
import com.crm.qualifier.service.ComplianceBureauService.ComplianceResponse;
import com.crm.qualifier.service.ComplianceBureauService.ComplianceStatus;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Validates a lead against OFAC/sanctions compliance lists.
 * Uses a file-based cache for performance and provides graceful degradation
 * when the compliance service is unavailable.
 */
public class ComplianceBureauValidator implements Validator {

    private static final String VALIDATOR_NAME = "ComplianceBureauValidator";
    private final ComplianceBureauService complianceService;
    private final ComplianceCache cache;

    public ComplianceBureauValidator(ComplianceBureauService complianceService, ComplianceCache cache) {
        this.complianceService = complianceService;
        this.cache = cache;
    }

    @Override
    public ValidationResult validate(Lead lead) {
        System.out.println("[COMPLIANCE] Checking compliance bureau for: " + lead.nationalId());

        // Check cache first
        Optional<ComplianceStatus> cachedStatus = cache.get(lead.nationalId());
        if (cachedStatus.isPresent()) {
            return buildResult(cachedStatus.get(), true);
        }

        // Cache miss — call external service
        System.out.println("[COMPLIANCE] Cache miss, calling external service...");
        try {
            ComplianceResponse response = complianceService.check(lead.nationalId());
            // Cache the successful response
            cache.put(lead.nationalId(), response.status());
            return buildResult(response.status(), false);
        } catch (RuntimeException e) {
            System.out.println("[COMPLIANCE] Service unavailable: " + e.getMessage());
            return new ValidationResult(false, VALIDATOR_NAME,
                "Compliance: SERVICE_UNAVAILABLE - Manual review required", LocalDateTime.now());
        }
    }

    private ValidationResult buildResult(ComplianceStatus status, boolean fromCache) {
        String cacheTag = fromCache ? " (cached)" : "";
        return switch (status) {
            case CLEAR -> {
                System.out.println("[COMPLIANCE] CLEAR - No sanctions match" + cacheTag);
                yield new ValidationResult(true, VALIDATOR_NAME,
                    "Compliance: CLEAR - No sanctions match" + cacheTag, LocalDateTime.now());
            }
            case FLAGGED -> {
                System.out.println("[COMPLIANCE] FLAGGED - Sanctions match found" + cacheTag);
                yield new ValidationResult(false, VALIDATOR_NAME,
                    "Compliance: FLAGGED - Sanctions match found" + cacheTag, LocalDateTime.now());
            }
        };
    }
}
