package com.crm.qualifier.orchestration;

import com.crm.qualifier.domain.*;
import com.crm.qualifier.validation.ComplianceBureauValidator;
import com.crm.qualifier.validation.JudicialRecordsValidator;
import com.crm.qualifier.validation.QualificationScoreValidator;
import com.crm.qualifier.validation.RegistryValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates the lead qualification pipeline.
 * Executes Registry and Judicial validations in parallel,
 * then runs Compliance and Score validations sequentially.
 */
public class LeadQualificationOrchestrator {

    private final RegistryValidator registryValidator;
    private final JudicialRecordsValidator judicialValidator;
    private final ComplianceBureauValidator complianceValidator;
    private final QualificationScoreValidator scoreValidator;

    public LeadQualificationOrchestrator(
            RegistryValidator registryValidator,
            JudicialRecordsValidator judicialValidator,
            ComplianceBureauValidator complianceValidator,
            QualificationScoreValidator scoreValidator) {
        this.registryValidator = registryValidator;
        this.judicialValidator = judicialValidator;
        this.complianceValidator = complianceValidator;
        this.scoreValidator = scoreValidator;
    }

    /**
     * Runs the full qualification pipeline on a lead.
     *
     * @param lead the lead to qualify
     * @return the pipeline result with status and all validation results
     */
    public PipelineResult qualify(Lead lead) {
        List<ValidationResult> results = new ArrayList<>();

        System.out.println("[PIPELINE] Starting qualification for: " + lead.nationalId());

        // Step 1: Run Registry + Judicial in parallel
        CompletableFuture<ValidationResult> registryFuture = CompletableFuture
            .supplyAsync(() -> registryValidator.validate(lead))
            .orTimeout(5, TimeUnit.SECONDS);

        CompletableFuture<ValidationResult> judicialFuture = CompletableFuture
            .supplyAsync(() -> judicialValidator.validate(lead))
            .orTimeout(5, TimeUnit.SECONDS);

        try {
            CompletableFuture.allOf(registryFuture, judicialFuture).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("[PIPELINE] Parallel validation timed out or failed: " + e.getMessage());
            results.add(new ValidationResult(false, "Pipeline",
                "Parallel validation error: " + e.getMessage(), LocalDateTime.now()));
            return new PipelineResult(results, QualificationStatus.REJECTED, null);
        }

        ValidationResult registryResult = registryFuture.join();
        ValidationResult judicialResult = judicialFuture.join();
        results.add(registryResult);
        results.add(judicialResult);

        // Short-circuit if either parallel validation failed
        if (!registryResult.success() || !judicialResult.success()) {
            System.out.println("[PIPELINE] Parallel validation failed. Result: REJECTED");
            return new PipelineResult(results, QualificationStatus.REJECTED, null);
        }

        System.out.println("[PIPELINE] Parallel validations passed. Running compliance check...");

        // Step 2: Run Compliance Bureau (sequential)
        ValidationResult complianceResult = complianceValidator.validate(lead);
        results.add(complianceResult);

        if (!complianceResult.success()) {
            if (complianceResult.message().contains("SERVICE_UNAVAILABLE")) {
                System.out.println("[PIPELINE] Compliance service unavailable. Result: MANUAL_REVIEW");
                return new PipelineResult(results, QualificationStatus.MANUAL_REVIEW, null);
            }
            System.out.println("[PIPELINE] Compliance check failed. Result: REJECTED");
            return new PipelineResult(results, QualificationStatus.REJECTED, null);
        }

        System.out.println("[PIPELINE] Compliance check passed. Calculating score...");

        // Step 3: Run Qualification Score (sequential)
        ValidationResult scoreResult = scoreValidator.validate(lead);
        results.add(scoreResult);

        if (!scoreResult.success()) {
            System.out.println("[PIPELINE] Score below threshold. Result: REJECTED");
            return new PipelineResult(results, QualificationStatus.REJECTED, null);
        }

        // All validations passed — create Prospect
        int score = scoreValidator.getLastScore();
        Prospect prospect = Prospect.fromLead(lead, score, LocalDateTime.now());

        System.out.println("[PIPELINE] Lead APPROVED as Prospect (score: " + score + ")");
        return new PipelineResult(results, QualificationStatus.APPROVED, prospect);
    }
}
