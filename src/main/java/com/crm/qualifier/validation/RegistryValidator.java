package com.crm.qualifier.validation;

import com.crm.qualifier.domain.Lead;
import com.crm.qualifier.domain.ValidationResult;
import com.crm.qualifier.service.RegistryService;
import com.crm.qualifier.service.RegistryService.RegistryResponse;
import com.crm.qualifier.service.RegistryService.RegistryStatus;

import java.time.LocalDateTime;

/**
 * Validates that a lead exists in the national registry and that their data matches.
 */
public class RegistryValidator implements Validator {

    private static final String VALIDATOR_NAME = "RegistryValidator";
    private final RegistryService registryService;

    public RegistryValidator(RegistryService registryService) {
        this.registryService = registryService;
    }

    @Override
    public ValidationResult validate(Lead lead) {
        System.out.println("[REGISTRY] Checking national registry for: " + lead.nationalId());

        try {
            RegistryResponse response = registryService.check(
                lead.nationalId(),
                lead.firstName(),
                lead.lastName(),
                lead.birthdate()
            );

            return switch (response.status()) {
                case MATCH -> {
                    System.out.println("[REGISTRY] MATCH - Person data verified");
                    yield new ValidationResult(true, VALIDATOR_NAME,
                        "Registry: MATCH - Person data verified", LocalDateTime.now());
                }
                case MISMATCH -> {
                    System.out.println("[REGISTRY] MISMATCH - Data does not match registry");
                    yield new ValidationResult(false, VALIDATOR_NAME,
                        "Registry: MISMATCH - Data does not match registry", LocalDateTime.now());
                }
                case NOT_FOUND -> {
                    System.out.println("[REGISTRY] NOT_FOUND - Person not in registry");
                    yield new ValidationResult(false, VALIDATOR_NAME,
                        "Registry: NOT_FOUND - Person not in registry", LocalDateTime.now());
                }
            };
        } catch (Exception e) {
            System.out.println("[REGISTRY] Error: " + e.getMessage());
            return new ValidationResult(false, VALIDATOR_NAME,
                "Registry: ERROR - " + e.getMessage(), LocalDateTime.now());
        }
    }
}
