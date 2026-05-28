package org.sihsalus.core.boot;

import java.util.Map;

record StaticModuleRuntimeState(
    boolean compiled,
    boolean configured,
    boolean springRegistered,
    boolean started,
    boolean databaseManaged,
    boolean databaseMigrated,
    int activeScheduledTasks,
    Map<String, Object> details) {}
