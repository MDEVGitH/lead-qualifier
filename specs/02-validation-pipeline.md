# Validation Pipeline Specification

## Overview

The lead qualification pipeline consists of 4 validation steps executed in a specific order.
Steps 1a and 1b run in parallel; steps 2 and 3 run sequentially after parallel completion.

```
Lead ──┬── 1a. Registry Validation ──┐
       │       (parallel)            ├── 2. Compliance Bureau ── 3. Score > 60 ── Prospect
       └── 1b. Judicial Records ─────┘      (cached/resilient)
```

---

## Validator Interface

All validators implement a common interface:

```java
public interface Validator {
    ValidationResult validate(Lead lead);
}
```

---

## Step 1a: Registry Validator

**Purpose:** Verify that the lead exists in the national registry and that their personal data matches.

**Input:** Lead (nationalId, firstName, lastName, birthdate)

**Process:**
1. Call RegistryService with lead's nationalId.
2. Receive RegistryResponse containing status and person data.
3. If status is NOT_FOUND → validation fails.
4. If status is MATCH → compare firstName, lastName, birthdate with lead data.
   - If all match → validation passes.
   - If any mismatch → validation fails with MISMATCH.
5. If status is MISMATCH → validation fails.

**Output:** ValidationResult
- Success: `{success: true, validatorName: "RegistryValidator", message: "Registry: MATCH - Person data verified"}`
- Failure (not found): `{success: false, validatorName: "RegistryValidator", message: "Registry: NOT_FOUND - Person not in registry"}`
- Failure (mismatch): `{success: false, validatorName: "RegistryValidator", message: "Registry: MISMATCH - Data does not match registry"}`

**Preconditions:**
- Lead must have a valid nationalId.

**Postconditions:**
- Always returns a ValidationResult (never throws).

**Error Cases:**
- Service timeout → validation fails with descriptive message.

---

## Step 1b: Judicial Records Validator

**Purpose:** Verify that the lead has no records in the national judicial archives.

**Input:** Lead (nationalId)

**Process:**
1. Call JudicialService with lead's nationalId.
2. Receive JudicialResponse with status.
3. If status is CLEAN → validation passes.
4. If status is HAS_RECORDS → validation fails.

**Output:** ValidationResult
- Success: `{success: true, validatorName: "JudicialRecordsValidator", message: "Judicial: CLEAN - No records found"}`
- Failure: `{success: false, validatorName: "JudicialRecordsValidator", message: "Judicial: HAS_RECORDS - Records found in archives"}`

**Preconditions:**
- Lead must have a valid nationalId.

**Postconditions:**
- Always returns a ValidationResult (never throws).

**Error Cases:**
- Service timeout → validation fails with descriptive message.

---

## Step 2: Compliance Bureau Validator

**Purpose:** Check the lead against OFAC/sanctions compliance lists. Uses caching for performance and graceful degradation when service is unavailable.

**Input:** Lead (nationalId)

**Process:**
1. Check ComplianceCache for existing entry by nationalId.
   - If cache HIT and not expired (TTL 24h) → use cached response, skip service call.
2. If cache MISS or expired → call ComplianceBureauService.
   - On success → cache the response, use it.
   - On failure (service throws exception) → return MANUAL_REVIEW result.
3. Evaluate response:
   - CLEAR → validation passes.
   - FLAGGED → validation fails.

**Output:** ValidationResult
- Success: `{success: true, validatorName: "ComplianceBureauValidator", message: "Compliance: CLEAR - No sanctions match"}`
- Failure (flagged): `{success: false, validatorName: "ComplianceBureauValidator", message: "Compliance: FLAGGED - Sanctions match found"}`
- Service down: `{success: false, validatorName: "ComplianceBureauValidator", message: "Compliance: SERVICE_UNAVAILABLE - Manual review required"}`

**Preconditions:**
- Steps 1a and 1b must have passed.

**Postconditions:**
- On service failure, result is not cached.
- On success, response is cached to file.
- Never throws exceptions to caller.

**Error Cases:**
- ComplianceBureauService throws RuntimeException → return MANUAL_REVIEW (not REJECTED).
- Cache file corrupted → treat as cache miss, proceed with service call.

---

## Step 3: Qualification Score Validator

**Purpose:** Generate a random qualification score and determine if the lead meets the threshold.

**Input:** Lead (nationalId)

**Process:**
1. Call QualificationScoreService to get a random score (0-100).
2. If score > 60 → validation passes.
3. If score <= 60 → validation fails.

**Output:** ValidationResult
- Success: `{success: true, validatorName: "QualificationScoreValidator", message: "Score: 75/100 - Above threshold (>60)"}`
- Failure: `{success: false, validatorName: "QualificationScoreValidator", message: "Score: 45/100 - Below threshold (>60)"}`

**Preconditions:**
- Steps 1a, 1b, and 2 must have passed (or compliance returned CLEAR).

**Postconditions:**
- Always returns a ValidationResult (never throws).
- Score is included in the message for audit purposes.

---

## Short-Circuit Behavior

- If Step 1a OR Step 1b fails → pipeline stops, returns REJECTED.
- If Step 2 service is down → pipeline stops, returns MANUAL_REVIEW.
- If Step 2 returns FLAGGED → pipeline stops, returns REJECTED.
- If Step 3 score <= 60 → pipeline returns REJECTED.
- Only if ALL steps pass → pipeline returns APPROVED with Prospect.
