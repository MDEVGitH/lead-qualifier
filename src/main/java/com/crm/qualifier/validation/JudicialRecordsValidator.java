package com.crm.qualifier.validation;

import com.crm.qualifier.domain.Lead;
import com.crm.qualifier.domain.ValidationResult;
import com.crm.qualifier.service.JudicialService;
import com.crm.qualifier.service.JudicialService.JudicialResponse;

import java.time.LocalDateTime;

/**
 * Validates that a lead has no records in the national judicial archives.
 */
public class JudicialRecordsValidator implements Validator {

    private static final String VALIDATOR_NAME = "JudicialRecordsValidator";
    private final JudicialService judicialService;

    public JudicialRecordsValidator(JudicialService judicialService) {
        this.judicialService = judicialService;
    }

    @Override
    public ValidationResult validate(Lead lead) {
        System.out.println("[JUDICIAL] Checking judicial records for: " + lead.nationalId());

        try {
            JudicialResponse response = judicialService.check(lead.nationalId());

            return switch (response.status()) {
                case CLEAN -> {
                    System.out.println("[JUDICIAL] CLEAN - No records found");
                    yield new ValidationResult(true, VALIDATOR_NAME,
                        "Judicial: CLEAN - No records found", LocalDateTime.now());
                }
                case HAS_RECORDS -> {
                    System.out.println("[JUDICIAL] HAS_RECORDS - Records found in archives");
                    yield new ValidationResult(false, VALIDATOR_NAME,
                        "Judicial: HAS_RECORDS - Records found in archives", LocalDateTime.now());
                }
            };
        } catch (Exception e) {
            System.out.println("[JUDICIAL] Error: " + e.getMessage());
            return new ValidationResult(false, VALIDATOR_NAME,
                "Judicial: ERROR - " + e.getMessage(), LocalDateTime.now());
        }
    }
}
