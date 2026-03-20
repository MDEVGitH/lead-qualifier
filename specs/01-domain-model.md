# Domain Model Specification

## Overview

This document defines all domain entities used in the lead qualification pipeline.
All entities are implemented as Java records (immutable value types).

---

## Entities

### Lead

Represents an unqualified potential customer entering the CRM pipeline.

| Field       | Type        | Constraints                                      |
|-------------|-------------|--------------------------------------------------|
| nationalId  | String      | Non-null, non-blank. Unique identifier.          |
| birthdate   | LocalDate   | Non-null. Must be in the past.                   |
| firstName   | String      | Non-null, non-blank.                             |
| lastName    | String      | Non-null, non-blank.                             |
| email       | String      | Non-null, non-blank. Must contain '@'.           |

**Invariants:**
- All fields are required (non-null).
- `nationalId` is the primary key for all external service lookups.
- `birthdate` must be before today's date.
- `email` must be a minimally valid format (contains '@').

---

### Prospect

Represents a qualified lead that has passed all validation steps.
Contains all Lead fields plus qualification metadata.

| Field              | Type           | Constraints                              |
|--------------------|----------------|------------------------------------------|
| nationalId         | String         | Inherited from Lead.                     |
| birthdate          | LocalDate      | Inherited from Lead.                     |
| firstName          | String         | Inherited from Lead.                     |
| lastName           | String         | Inherited from Lead.                     |
| email              | String         | Inherited from Lead.                     |
| qualificationScore | int            | 0-100. Must be > 60 for APPROVED status. |
| qualifiedAt        | LocalDateTime  | Timestamp when qualification completed.  |

**Invariants:**
- All Lead invariants apply.
- `qualificationScore` is in range [0, 100].
- `qualifiedAt` is non-null, set at pipeline completion time.
- A Prospect only exists if pipeline result is APPROVED.

---

### ValidationResult

Represents the outcome of a single validation step in the pipeline.

| Field         | Type           | Constraints                                  |
|---------------|----------------|----------------------------------------------|
| success       | boolean        | true if validation passed, false otherwise.  |
| validatorName | String         | Non-null. Identifies which validator ran.    |
| message       | String         | Non-null. Human-readable result description. |
| timestamp     | LocalDateTime  | Non-null. When the validation completed.     |

**Invariants:**
- `validatorName` matches one of: "RegistryValidator", "JudicialRecordsValidator", "ComplianceBureauValidator", "QualificationScoreValidator".
- `message` provides actionable detail (e.g., "Registry: MATCH", "Judicial: HAS_RECORDS").

---

### QualificationStatus (Enum)

Represents the final outcome of the qualification pipeline.

| Value          | Description                                                    |
|----------------|----------------------------------------------------------------|
| APPROVED       | All validations passed, score > 60. Lead becomes Prospect.     |
| REJECTED       | One or more validations failed. Lead is not qualified.          |
| MANUAL_REVIEW  | Compliance service was unavailable. Requires human review.      |

---

### PipelineResult

Represents the complete result of running the qualification pipeline on a Lead.

| Field              | Type                     | Constraints                                    |
|--------------------|--------------------------|------------------------------------------------|
| validationResults  | List<ValidationResult>   | Non-null. Contains results of all steps run.   |
| status             | QualificationStatus      | Non-null. Final pipeline outcome.              |
| prospect           | Prospect (optional)      | Present only when status is APPROVED.          |

**Invariants:**
- `validationResults` is never null (may be empty if pipeline fails at start).
- When `status` is APPROVED, `prospect` must be non-null.
- When `status` is REJECTED or MANUAL_REVIEW, `prospect` must be null.
- `validationResults` are ordered chronologically (order of execution).

---

## Design Decisions

1. **Java Records**: All domain types use Java 17 records for immutability and concise value semantics.
2. **Optional Prospect**: `PipelineResult` uses `Prospect` directly (nullable) rather than `Optional<Prospect>` in the record definition, since records don't support Optional fields cleanly with serialization. Accessor methods may return Optional.
3. **Timestamp consistency**: All timestamps use `LocalDateTime` for consistency within the pipeline.
