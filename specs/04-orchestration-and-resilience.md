# Orchestration and Resilience Specification

## Overview

This document specifies how the qualification pipeline is orchestrated, including parallel execution, caching strategy, resilience patterns, and timeout handling.

---

## Pipeline Orchestration

### LeadQualificationOrchestrator

**Main method:** `PipelineResult qualify(Lead lead)`

**Execution Flow:**

```
1. Log: "[PIPELINE] Starting qualification for {nationalId}"
2. PARALLEL:
   a. CompletableFuture<ValidationResult> registryFuture = supplyAsync(() -> registryValidator.validate(lead))
   b. CompletableFuture<ValidationResult> judicialFuture = supplyAsync(() -> judicialValidator.validate(lead))
3. Wait: CompletableFuture.allOf(registryFuture, judicialFuture).join()
4. Collect results from both futures
5. SHORT-CIRCUIT: If registry OR judicial failed → return PipelineResult(REJECTED, results)
6. Log: "[PIPELINE] Parallel validations passed. Running compliance check..."
7. SEQUENTIAL: complianceResult = complianceBureauValidator.validate(lead)
8. SHORT-CIRCUIT:
   - If compliance message contains "SERVICE_UNAVAILABLE" → return PipelineResult(MANUAL_REVIEW, results)
   - If compliance failed (FLAGGED) → return PipelineResult(REJECTED, results)
9. Log: "[PIPELINE] Compliance check passed. Calculating score..."
10. SEQUENTIAL: scoreResult = qualificationScoreValidator.validate(lead)
11. SHORT-CIRCUIT: If score failed → return PipelineResult(REJECTED, results)
12. Build Prospect from Lead + score
13. Log: "[PIPELINE] Lead APPROVED as Prospect"
14. Return PipelineResult(APPROVED, results, prospect)
```

### Parallel Execution Details

- Use `CompletableFuture.supplyAsync()` with the default ForkJoinPool.
- Both Registry and Judicial validations start simultaneously.
- `CompletableFuture.allOf()` ensures both complete before proceeding.
- Results are retrieved with `.join()` (which re-throws exceptions as CompletionException).

### Short-Circuit Logic

The pipeline stops at the first failure point:
1. If any parallel validation fails → REJECTED (both results included in output).
2. If compliance service unavailable → MANUAL_REVIEW (all results so far included).
3. If compliance flagged → REJECTED.
4. If score too low → REJECTED.

Only if ALL four validations pass does the pipeline return APPROVED.

---

## Compliance Cache Specification

### File Location
- Path: `./data/compliance-cache.json`
- Created automatically if not exists.
- `data/` directory created automatically if not exists.

### Cache Structure (JSON)
```json
{
  "entries": {
    "12345678": {
      "response": {
        "status": "CLEAR"
      },
      "timestamp": "2024-01-15T10:30:00"
    },
    "87654321": {
      "response": {
        "status": "FLAGGED"
      },
      "timestamp": "2024-01-15T09:00:00"
    }
  }
}
```

### Cache Key
- `nationalId` (String)

### Cache Value
- `response`: ComplianceResponse object (status field)
- `timestamp`: ISO LocalDateTime when the entry was cached

### TTL (Time-To-Live)
- 24 hours from `timestamp`
- Expired entries are treated as cache misses
- Expired entries are NOT proactively cleaned (lazy eviction)

### Cache Operations

**get(String nationalId) → Optional<ComplianceResponse>**
1. Read cache file from disk.
2. Look up entry by nationalId.
3. If found and not expired (timestamp + 24h > now) → return Optional.of(response).
4. If found but expired → return Optional.empty().
5. If not found → return Optional.empty().

**put(String nationalId, ComplianceResponse response)**
1. Read current cache file (or create empty structure).
2. Add/update entry with current timestamp.
3. Write entire cache back to file.
4. Ensure `data/` directory exists before writing.

### Concurrency
- File I/O is NOT concurrent-safe (acceptable for CLI single-process use).
- Reads and writes are serialized within a single pipeline execution.

### Error Handling
- If cache file is corrupted (invalid JSON) → treat as empty cache, log warning.
- If file I/O fails → treat as cache miss, log warning, do not crash.
- Service failures are NOT cached (only successful responses).

---

## Resilience Patterns

### Compliance Service Failure
- **Pattern:** Graceful degradation
- **Behavior:** When ComplianceBureauService throws RuntimeException, the ComplianceBureauValidator catches it and returns a ValidationResult with `success: false` and message containing "SERVICE_UNAVAILABLE".
- **Orchestrator behavior:** Detects SERVICE_UNAVAILABLE in message and returns MANUAL_REVIEW instead of REJECTED.
- **Rationale:** A service outage should not auto-reject a lead; human review is safer.

### Timeout Strategy
- **Per-service timeout:** 5 seconds max per external call.
- **Implementation:** CompletableFuture with `.orTimeout(5, TimeUnit.SECONDS)` or equivalent handling.
- **On timeout:** Treat as validation failure with descriptive message.

### Logging
All pipeline steps log to stdout with prefixed indicators:
- `[PIPELINE]` — Orchestration-level messages
- `[REGISTRY]` — Registry validation step
- `[JUDICIAL]` — Judicial records check step
- `[COMPLIANCE]` — Compliance bureau step
- `[SCORE]` — Qualification score step
- `[CACHE]` — Cache operations

---

## Exit Codes

The CLI process exits with:
- `0` — APPROVED (lead became a prospect)
- `1` — REJECTED (one or more validations failed)
- `2` — MANUAL_REVIEW (compliance service unavailable)
