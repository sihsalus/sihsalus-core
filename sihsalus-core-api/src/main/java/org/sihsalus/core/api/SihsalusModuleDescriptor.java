package org.sihsalus.core.api;

public record SihsalusModuleDescriptor(
        String id, String sourceModule, String baselineVersion, SihsalusModuleStatus status) {}
