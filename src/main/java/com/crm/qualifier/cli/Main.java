package com.crm.qualifier.cli;

import com.crm.qualifier.cache.ComplianceCache;
import com.crm.qualifier.domain.*;
import com.crm.qualifier.orchestration.LeadQualificationOrchestrator;
import com.crm.qualifier.service.*;
import com.crm.qualifier.validation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * CLI entry point for the lead qualification pipeline.
 * Supports both command-line argument mode and interactive mode.
 *
 * Exit codes: 0=APPROVED, 1=REJECTED, 2=MANUAL_REVIEW
 */
public class Main {

    public static void main(String[] args) {
        Lead lead;

        if (args.length >= 5) {
            lead = parseArgs(args);
        } else {
            lead = interactiveMode();
        }

        if (lead == null) {
            System.err.println("Error: Could not create lead. Please check your input.");
            System.exit(1);
        }

        // Wire up services → validators → orchestrator
        RegistryService registryService = new RegistryService();
        JudicialService judicialService = new JudicialService();
        ComplianceBureauService complianceService = new ComplianceBureauService();
        QualificationScoreService scoreService = new QualificationScoreService();
        ComplianceCache complianceCache = new ComplianceCache();

        RegistryValidator registryValidator = new RegistryValidator(registryService);
        JudicialRecordsValidator judicialValidator = new JudicialRecordsValidator(judicialService);
        ComplianceBureauValidator complianceValidator = new ComplianceBureauValidator(complianceService, complianceCache);
        QualificationScoreValidator scoreValidator = new QualificationScoreValidator(scoreService);

        LeadQualificationOrchestrator orchestrator = new LeadQualificationOrchestrator(
            registryValidator, judicialValidator, complianceValidator, scoreValidator
        );

        // Run the pipeline
        PipelineResult result = orchestrator.qualify(lead);

        // Print formatted output
        printResult(lead, result);

        // Exit with appropriate code
        System.exit(exitCode(result.status()));
    }

    /**
     * Parses command-line arguments in the format --key=value.
     */
    static Lead parseArgs(String[] args) {
        String nationalId = null;
        String firstName = null;
        String lastName = null;
        String birthdate = null;
        String email = null;

        for (String arg : args) {
            if (arg.startsWith("--nationalId=")) {
                nationalId = arg.substring("--nationalId=".length());
            } else if (arg.startsWith("--firstName=")) {
                firstName = arg.substring("--firstName=".length());
            } else if (arg.startsWith("--lastName=")) {
                lastName = arg.substring("--lastName=".length());
            } else if (arg.startsWith("--birthdate=")) {
                birthdate = arg.substring("--birthdate=".length());
            } else if (arg.startsWith("--email=")) {
                email = arg.substring("--email=".length());
            }
        }

        if (nationalId == null || firstName == null || lastName == null || birthdate == null || email == null) {
            System.err.println("Usage: lead-qualifier --nationalId=X --firstName=X --lastName=X --birthdate=YYYY-MM-DD --email=X");
            return null;
        }

        try {
            LocalDate bd = LocalDate.parse(birthdate);
            return new Lead(nationalId, bd, firstName, lastName, email);
        } catch (DateTimeParseException e) {
            System.err.println("Error: Invalid birthdate format. Use YYYY-MM-DD.");
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Prompts the user for each field interactively.
     */
    static Lead interactiveMode() {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("=== Lead Qualification - Interactive Mode ===");
        System.out.println();

        try {
            System.out.print("National ID: ");
            String nationalId = scanner.nextLine().trim();

            System.out.print("First Name: ");
            String firstName = scanner.nextLine().trim();

            System.out.print("Last Name: ");
            String lastName = scanner.nextLine().trim();

            System.out.print("Birthdate (YYYY-MM-DD): ");
            String birthdateStr = scanner.nextLine().trim();
            LocalDate birthdate = LocalDate.parse(birthdateStr);

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            System.out.println();
            return new Lead(nationalId, birthdate, firstName, lastName, email);
        } catch (DateTimeParseException e) {
            System.err.println("Error: Invalid birthdate format. Use YYYY-MM-DD.");
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Prints the pipeline result in a formatted table.
     */
    static void printResult(Lead lead, PipelineResult result) {
        System.out.println();
        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        System.out.println(" LEAD QUALIFICATION PIPELINE");
        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        System.out.println("Lead: " + lead.firstName() + " " + lead.lastName() + " (ID: " + lead.nationalId() + ")");
        System.out.println("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");

        for (ValidationResult vr : result.validationResults()) {
            String icon = vr.success() ? "\u2713" : "\u2717";
            String status = vr.success() ? "PASSED" : "FAILED";
            String detail = extractDetail(vr);
            System.out.printf("[%s] %-24s - %s%s%n", icon, friendlyName(vr.validatorName()), status, detail);
        }

        System.out.println("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        System.out.println("Result: " + result.status());

        if (result.status() == QualificationStatus.MANUAL_REVIEW) {
            System.out.println("Action: Lead requires manual compliance review.");
        }

        result.getProspect().ifPresent(p ->
            System.out.println("Prospect created with score: " + p.qualificationScore() + " at " + p.qualifiedAt())
        );

        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
    }

    /**
     * Converts validator class names to friendly display names.
     */
    private static String friendlyName(String validatorName) {
        return switch (validatorName) {
            case "RegistryValidator" -> "Registry Validation";
            case "JudicialRecordsValidator" -> "Judicial Records";
            case "ComplianceBureauValidator" -> "Compliance Bureau";
            case "QualificationScoreValidator" -> "Qualification Score";
            default -> validatorName;
        };
    }

    /**
     * Extracts additional detail from the validation message for display.
     */
    private static String extractDetail(ValidationResult vr) {
        String msg = vr.message();
        if (msg.contains("(cached)")) return " (cached)";
        if (msg.contains("Score:")) {
            // Extract score value, e.g., "Score: 78/100 ..."
            int idx = msg.indexOf("Score: ");
            if (idx >= 0) {
                String scorePart = msg.substring(idx);
                int slashIdx = scorePart.indexOf("/");
                if (slashIdx > 7) {
                    return " (score: " + scorePart.substring(7, slashIdx) + ")";
                }
            }
        }
        return "";
    }

    /**
     * Maps QualificationStatus to process exit code.
     */
    static int exitCode(QualificationStatus status) {
        return switch (status) {
            case APPROVED -> 0;
            case REJECTED -> 1;
            case MANUAL_REVIEW -> 2;
        };
    }
}
