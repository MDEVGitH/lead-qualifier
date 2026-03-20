package com.crm.qualifier.cache;

import com.crm.qualifier.service.ComplianceBureauService.ComplianceStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Compliance Cache Tests")
class ComplianceCacheTest {

    @TempDir
    Path tempDir;

    private Path cacheFile;

    @BeforeEach
    void setUp() {
        cacheFile = tempDir.resolve("cache.json");
    }

    @Test
    @DisplayName("Should return empty for unknown nationalId")
    void shouldReturnEmptyForUnknownId() {
        ComplianceCache cache = new ComplianceCache(cacheFile, Duration.ofHours(24));
        Optional<ComplianceStatus> result = cache.get("unknown-id");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should put and get a CLEAR entry")
    void shouldPutAndGetClear() {
        ComplianceCache cache = new ComplianceCache(cacheFile, Duration.ofHours(24));
        cache.put("123", ComplianceStatus.CLEAR);
        Optional<ComplianceStatus> result = cache.get("123");
        assertTrue(result.isPresent());
        assertEquals(ComplianceStatus.CLEAR, result.get());
    }

    @Test
    @DisplayName("Should put and get a FLAGGED entry")
    void shouldPutAndGetFlagged() {
        ComplianceCache cache = new ComplianceCache(cacheFile, Duration.ofHours(24));
        cache.put("456", ComplianceStatus.FLAGGED);
        Optional<ComplianceStatus> result = cache.get("456");
        assertTrue(result.isPresent());
        assertEquals(ComplianceStatus.FLAGGED, result.get());
    }

    @Test
    @DisplayName("Should expire entries after TTL")
    void shouldExpireAfterTtl() throws InterruptedException {
        // Use a very short TTL for testing
        ComplianceCache cache = new ComplianceCache(cacheFile, Duration.ofMillis(100));
        cache.put("123", ComplianceStatus.CLEAR);

        // Should be available immediately
        assertTrue(cache.get("123").isPresent());

        // Wait for expiration
        Thread.sleep(200);

        // Should be expired now
        assertTrue(cache.get("123").isEmpty());
    }

    @Test
    @DisplayName("Should create the cache file on first write")
    void shouldCreateFileOnWrite() {
        assertFalse(Files.exists(cacheFile));
        ComplianceCache cache = new ComplianceCache(cacheFile, Duration.ofHours(24));
        cache.put("123", ComplianceStatus.CLEAR);
        assertTrue(Files.exists(cacheFile));
    }

    @Test
    @DisplayName("Should persist across cache instances")
    void shouldPersistAcrossInstances() {
        ComplianceCache cache1 = new ComplianceCache(cacheFile, Duration.ofHours(24));
        cache1.put("123", ComplianceStatus.CLEAR);

        // Create a new instance pointing to the same file
        ComplianceCache cache2 = new ComplianceCache(cacheFile, Duration.ofHours(24));
        Optional<ComplianceStatus> result = cache2.get("123");
        assertTrue(result.isPresent());
        assertEquals(ComplianceStatus.CLEAR, result.get());
    }

    @Test
    @DisplayName("Should handle corrupted cache file gracefully")
    void shouldHandleCorruptedFile() throws IOException {
        Files.writeString(cacheFile, "this is not valid JSON");
        ComplianceCache cache = new ComplianceCache(cacheFile, Duration.ofHours(24));

        // Should treat as empty (cache miss) without crashing
        Optional<ComplianceStatus> result = cache.get("123");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should create parent directories if needed")
    void shouldCreateParentDirectories() {
        Path nestedFile = tempDir.resolve("nested/dir/cache.json");
        ComplianceCache cache = new ComplianceCache(nestedFile, Duration.ofHours(24));
        cache.put("123", ComplianceStatus.CLEAR);
        assertTrue(Files.exists(nestedFile));
    }

    @Test
    @DisplayName("Should store multiple entries")
    void shouldStoreMultipleEntries() {
        ComplianceCache cache = new ComplianceCache(cacheFile, Duration.ofHours(24));
        cache.put("111", ComplianceStatus.CLEAR);
        cache.put("222", ComplianceStatus.FLAGGED);
        cache.put("333", ComplianceStatus.CLEAR);

        assertEquals(ComplianceStatus.CLEAR, cache.get("111").orElse(null));
        assertEquals(ComplianceStatus.FLAGGED, cache.get("222").orElse(null));
        assertEquals(ComplianceStatus.CLEAR, cache.get("333").orElse(null));
    }
}
