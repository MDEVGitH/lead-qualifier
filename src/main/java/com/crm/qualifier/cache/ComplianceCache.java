package com.crm.qualifier.cache;

import com.crm.qualifier.service.ComplianceBureauService.ComplianceStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * File-based cache for compliance bureau responses.
 * Stores entries as JSON, keyed by nationalId, with a configurable TTL.
 * Thread-safe via synchronized methods.
 */
public class ComplianceCache {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final Path cacheFilePath;
    private final Duration ttl;
    private final Gson gson;

    /**
     * Creates a cache with the default file path (./data/compliance-cache.json) and 24h TTL.
     */
    public ComplianceCache() {
        this(Path.of("./data/compliance-cache.json"), DEFAULT_TTL);
    }

    /**
     * Creates a cache with a custom file path and TTL.
     *
     * @param cacheFilePath path to the cache JSON file
     * @param ttl           time-to-live for cache entries
     */
    public ComplianceCache(Path cacheFilePath, Duration ttl) {
        this.cacheFilePath = cacheFilePath;
        this.ttl = ttl;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Retrieves a cached compliance status for the given nationalId.
     * Returns empty if the entry is not found, expired, or the cache file is corrupted.
     *
     * @param nationalId the person's national ID
     * @return the cached ComplianceStatus, or empty
     */
    public synchronized Optional<ComplianceStatus> get(String nationalId) {
        Map<String, CacheEntry> entries = readCache();
        CacheEntry entry = entries.get(nationalId);

        if (entry == null) {
            return Optional.empty();
        }

        try {
            LocalDateTime cachedAt = LocalDateTime.parse(entry.timestamp());
            if (cachedAt.plus(ttl).isAfter(LocalDateTime.now())) {
                System.out.println("[CACHE] Hit for nationalId: " + nationalId + " (status: " + entry.status() + ")");
                return Optional.of(entry.status());
            } else {
                System.out.println("[CACHE] Expired entry for nationalId: " + nationalId);
                return Optional.empty();
            }
        } catch (DateTimeParseException e) {
            System.out.println("[CACHE] Warning: Invalid timestamp for nationalId: " + nationalId);
            return Optional.empty();
        }
    }

    /**
     * Stores a compliance status in the cache for the given nationalId.
     * Creates the parent directory and file if they don't exist.
     *
     * @param nationalId the person's national ID
     * @param status     the compliance status to cache
     */
    public synchronized void put(String nationalId, ComplianceStatus status) {
        Map<String, CacheEntry> entries = readCache();
        entries.put(nationalId, new CacheEntry(status, LocalDateTime.now().toString()));
        writeCache(entries);
        System.out.println("[CACHE] Stored entry for nationalId: " + nationalId + " (status: " + status + ")");
    }

    /**
     * Reads the cache file from disk.
     * Returns an empty map if the file doesn't exist or is corrupted.
     */
    private Map<String, CacheEntry> readCache() {
        if (!Files.exists(cacheFilePath)) {
            return new HashMap<>();
        }

        try {
            String json = Files.readString(cacheFilePath);
            Type type = new TypeToken<Map<String, CacheEntry>>() {}.getType();
            Map<String, CacheEntry> entries = gson.fromJson(json, type);
            return entries != null ? new HashMap<>(entries) : new HashMap<>();
        } catch (IOException e) {
            System.out.println("[CACHE] Warning: Could not read cache file: " + e.getMessage());
            return new HashMap<>();
        } catch (Exception e) {
            System.out.println("[CACHE] Warning: Cache file corrupted, treating as empty: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Writes the cache entries to disk as JSON.
     * Creates parent directories if they don't exist.
     */
    private void writeCache(Map<String, CacheEntry> entries) {
        try {
            Path parentDir = cacheFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            String json = gson.toJson(entries);
            Files.writeString(cacheFilePath, json);
        } catch (IOException e) {
            System.out.println("[CACHE] Warning: Could not write cache file: " + e.getMessage());
        }
    }
}
