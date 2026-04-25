package com.ma3auka.alaaggro.util;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory set of player UUIDs exempt from aggro. Not persisted across restarts (PRD §3.10).
 */
public final class ExemptRegistry {
    private static final Set<UUID> EXEMPT = ConcurrentHashMap.newKeySet();

    private ExemptRegistry() {}

    public static boolean add(UUID id) {
        return EXEMPT.add(id);
    }

    public static boolean remove(UUID id) {
        return EXEMPT.remove(id);
    }

    public static boolean isExempt(UUID id) {
        return EXEMPT.contains(id);
    }

    public static Set<UUID> view() {
        return Collections.unmodifiableSet(EXEMPT);
    }

    public static void clear() {
        EXEMPT.clear();
    }
}
