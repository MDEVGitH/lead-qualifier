# External Services Specification

## Overview

All external services are simulated Java classes that mimic real-world behavior:
- Network latency via `Thread.sleep()` with randomized delays
- Probabilistic responses to simulate real-world distributions
- Exception throwing to simulate service outages

These services are NOT interfaces — they are concrete classes that simulate external dependencies.

---

## Common Patterns

All services follow these patterns:
- Simulate network latency with `Thread.sleep(random within range)`
- Use `java.util.Random` for probabilistic outcomes
- Accept a nationalId (String) as the primary lookup key
- Return typed response records

---

## RegistryService

**Purpose:** Simulates a national registry lookup service.

**Method:** `RegistryResponse check(String nationalId, String firstName, String lastName, LocalDate birthdate)`

**Latency:** 200-800ms (uniformly distributed)

**Response Distribution:**
- ~70% → MATCH (person found, data matches)
- ~20% → MISMATCH (person found, data does not match)
- ~10% → NOT_FOUND (person not in registry)

**Response Record:**
```java
public record RegistryResponse(
    Status status,
    String firstName,   // null if NOT_FOUND
    String lastName,    // null if NOT_FOUND
    LocalDate birthdate // null if NOT_FOUND
) {
    public enum Status { MATCH, MISMATCH, NOT_FOUND }
}
```

**Behavior:**
- On MATCH: returns the exact input data as person data.
- On MISMATCH: returns slightly different person data (different first name).
- On NOT_FOUND: returns null for all person fields.

---

## JudicialService

**Purpose:** Simulates a national judicial archives lookup service.

**Method:** `JudicialResponse check(String nationalId)`

**Latency:** 300-1000ms (uniformly distributed)

**Response Distribution:**
- ~85% → CLEAN (no judicial records)
- ~15% → HAS_RECORDS (judicial records found)

**Response Record:**
```java
public record JudicialResponse(
    Status status
) {
    public enum Status { CLEAN, HAS_RECORDS }
}
```

---

## ComplianceBureauService

**Purpose:** Simulates an OFAC/sanctions compliance checking service.

**Method:** `ComplianceResponse check(String nationalId)`

**Latency:** 100-500ms (uniformly distributed)

**Response Distribution:**
- ~80% → CLEAR (no sanctions match)
- ~10% → FLAGGED (sanctions match found)
- ~10% → throws `RuntimeException("Compliance Bureau service unavailable")`

**Response Record:**
```java
public record ComplianceResponse(
    Status status
) {
    public enum Status { CLEAR, FLAGGED }
}
```

**Error Behavior:**
- When service is "down", throws `RuntimeException` (not a checked exception).
- Callers MUST handle this gracefully (never propagate to user as a crash).

---

## QualificationScoreService

**Purpose:** Generates a random qualification score for a lead.

**Method:** `int score(String nationalId)`

**Latency:** None (instant — this is an internal scoring function).

**Response:** Random integer in range [0, 100] inclusive.

**Distribution:** Uniform random.

---

## Thread Safety

All services are stateless and thread-safe. They can be called concurrently from multiple threads (as required by the parallel execution of Steps 1a and 1b).

---

## Testing Considerations

- Services use `java.util.Random` which can be seeded for deterministic tests.
- Constructor should accept an optional `Random` instance for testability.
- Latency simulation means tests involving these services will have real delays. Consider using shorter delays or mocking in unit tests.
