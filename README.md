# Lead Qualifier

Automated lead-to-prospect qualification orchestrator. A Java 17 CLI application that runs leads through a multi-step validation pipeline, leveraging parallel execution, file-based caching, and resilient service integration.

## Pipeline Architecture

```
Lead ──┬── 1a. Registry Validation ──┐
       │       (parallel)            ├── 2. Compliance Bureau ── 3. Score > 60 ── Prospect
       └── 1b. Judicial Records ─────┘      (cached/resilient)
```

**Lead data**: nationalId, birthdate, firstName, lastName, email

### Validation Steps

| Step | Name | Execution | Description |
|------|------|-----------|-------------|
| 1a | Registry Validation | Parallel | Verify person exists in national registry and data matches |
| 1b | Judicial Records | Parallel | Check for judicial records in national archives |
| 2 | Compliance Bureau | Sequential | OFAC/sanctions check with file cache and graceful degradation |
| 3 | Qualification Score | Sequential | Random score 0-100, must be > 60 to convert |

### Pipeline Outcomes

| Status | Meaning |
|--------|---------|
| **APPROVED** | All validations passed, score > 60. Lead becomes a Prospect. |
| **REJECTED** | One or more validations failed. |
| **MANUAL_REVIEW** | Compliance service was unavailable. Requires human review. |

## Design Decisions

### CompletableFuture for Parallel Execution
Steps 1a (Registry) and 1b (Judicial) run simultaneously using `CompletableFuture.supplyAsync()`. The pipeline waits for both with `CompletableFuture.allOf()` before proceeding to sequential steps. This reduces total pipeline latency by overlapping the two slowest external calls.

### File-Based Compliance Cache
Compliance Bureau responses are cached to `./data/compliance-cache.json` with a 24-hour TTL. This avoids redundant external calls for recently checked leads. The cache uses Gson for JSON serialization and synchronized methods for thread safety.

### Resilience: Graceful Degradation
When the Compliance Bureau service throws a `RuntimeException` (simulating downtime), the pipeline returns `MANUAL_REVIEW` instead of crashing or auto-rejecting. This ensures service outages do not silently reject valid leads.

### Simulated External Services
All external services are concrete Java classes that simulate real-world behavior:
- **Latency**: `Thread.sleep()` with randomized delays (200-1000ms)
- **Probabilistic outcomes**: `ThreadLocalRandom` for realistic response distributions
- **Failure simulation**: ComplianceBureauService throws exceptions ~10% of the time

### Java Records
All domain types (Lead, Prospect, ValidationResult, PipelineResult) and service responses use Java 17 records for immutability, concise syntax, and automatic equals/hashCode/toString.

## Build

**Prerequisites**: Java 17+, Maven 3.8+

```bash
mvn clean package
```

This produces `target/lead-qualifier-1.0.0-SNAPSHOT.jar`.

## Run

### Command-Line Arguments

```bash
java -jar target/lead-qualifier-1.0.0-SNAPSHOT.jar \
  --nationalId=123456789 \
  --firstName=John \
  --lastName=Doe \
  --birthdate=1990-05-15 \
  --email=john.doe@example.com
```

### Interactive Mode

Run without arguments to enter interactive mode:

```bash
java -jar target/lead-qualifier-1.0.0-SNAPSHOT.jar
```

You will be prompted for each field.

### Sample Output

```
══════════════════════════════════════
 LEAD QUALIFICATION PIPELINE
══════════════════════════════════════
Lead: John Doe (ID: 123456789)
──────────────────────────────────────
[✓] Registry Validation      - PASSED
[✓] Judicial Records         - PASSED
[✓] Compliance Bureau        - PASSED (cached)
[✓] Qualification Score      - PASSED (score: 78)
──────────────────────────────────────
Result: APPROVED
══════════════════════════════════════
```

### Exit Codes

| Code | Status |
|------|--------|
| 0 | APPROVED |
| 1 | REJECTED |
| 2 | MANUAL_REVIEW |

## Test

```bash
mvn test
```

The test suite includes:
- **Domain model tests**: Validation of Lead record invariants (null checks, blank checks, date/email validation)
- **Service tests**: Verification that simulated services produce expected response distributions and latency
- **Cache tests**: Put/get, TTL expiration, file persistence, corruption handling
- **Validator tests**: Unit tests with mock services for each validator (match, mismatch, not found, service down)
- **Orchestrator tests**: Full pipeline integration tests covering all outcomes (APPROVED, REJECTED, MANUAL_REVIEW), boundary conditions (score=60, score=61), and real-service integration runs

## Project Structure

```
lead-qualifier/
├── pom.xml
├── README.md
├── specs/
│   ├── 01-domain-model.md
│   ├── 02-validation-pipeline.md
│   ├── 03-external-services.md
│   └── 04-orchestration-and-resilience.md
├── src/
│   ├── main/java/com/crm/qualifier/
│   │   ├── cache/
│   │   │   ├── CacheEntry.java
│   │   │   └── ComplianceCache.java
│   │   ├── cli/
│   │   │   └── Main.java
│   │   ├── domain/
│   │   │   ├── Lead.java
│   │   │   ├── PipelineResult.java
│   │   │   ├── Prospect.java
│   │   │   ├── QualificationStatus.java
│   │   │   └── ValidationResult.java
│   │   ├── orchestration/
│   │   │   └── LeadQualificationOrchestrator.java
│   │   ├── service/
│   │   │   ├── ComplianceBureauService.java
│   │   │   ├── JudicialService.java
│   │   │   ├── QualificationScoreService.java
│   │   │   └── RegistryService.java
│   │   └── validation/
│   │       ├── ComplianceBureauValidator.java
│   │       ├── JudicialRecordsValidator.java
│   │       ├── QualificationScoreValidator.java
│   │       ├── RegistryValidator.java
│   │       └── Validator.java
│   └── test/java/com/crm/qualifier/
│       ├── cache/
│       │   └── ComplianceCacheTest.java
│       ├── domain/
│       │   └── LeadTest.java
│       ├── orchestration/
│       │   └── LeadQualificationOrchestratorTest.java
│       ├── service/
│       │   ├── ComplianceBureauServiceTest.java
│       │   └── RegistryServiceTest.java
│       └── validation/
│           ├── ComplianceBureauValidatorTest.java
│           └── RegistryValidatorTest.java
└── data/
    └── compliance-cache.json  (created at runtime)
```

## Spec-Driven Development

This project follows a spec-driven development process:

1. **Specifications first**: All four spec documents (`specs/`) were written and committed before any implementation code.
2. **Domain model**: Records and enums were implemented directly from the domain spec.
3. **Services**: Simulated external services follow the contracts defined in the external services spec.
4. **Validators**: Each validator implements the preconditions, postconditions, and error handling defined in the validation pipeline spec.
5. **Orchestrator**: The pipeline flow, short-circuit logic, and resilience patterns follow the orchestration spec exactly.
6. **Tests**: Test cases verify the behavior described in each specification.
