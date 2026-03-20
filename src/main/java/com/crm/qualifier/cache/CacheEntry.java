package com.crm.qualifier.cache;

import com.crm.qualifier.service.ComplianceBureauService.ComplianceStatus;

/**
 * Represents a single cached compliance check result.
 * Used for JSON serialization via Gson.
 */
public record CacheEntry(
    ComplianceStatus status,
    String timestamp
) {}
