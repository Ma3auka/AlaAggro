package com.ma3auka.alaaggro.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players that mobs must leave alone ({@code /alaaggro exempt}).
 *
 * <p>Read from the server tick and from targeting predicates, written from command handlers, hence
 * the concurrent set — a plain {@code HashSet} here races and silently drops entries, which shows
 * up as an exempt player being attacked at random. {@code ExemptRegistryTest} pins that.
 *
 * <p>The set lives in memory for speed; persistence across restarts is layered on top by
 * {@code ExemptStorage}, which calls {@link #replaceAll} on world load and listens for changes.
 * {@link #clear()} on server stop is what keeps a single-player exemption from following the player
 * into a different world.
 */
public final class ExemptRegistry {

    private static final Set<UUID> EXEMPT = ConcurrentHashMap.newKeySet();
    private static volatile Runnable changeListener;

    private ExemptRegistry() {}

    /** @return true if the player was not already exempt */
    public static boolean add(UUID id) {
        boolean changed = EXEMPT.add(id);
        if (changed) notifyChanged();
        return changed;
    }

    /** @return true if the player was actually on the list */
    public static boolean remove(UUID id) {
        boolean changed = EXEMPT.remove(id);
        if (changed) notifyChanged();
        return changed;
    }

    public static boolean isExempt(UUID id) {
        return EXEMPT.contains(id);
    }

    /** Live, unmodifiable view — callers cannot corrupt the backing set. */
    public static Set<UUID> view() {
        return Collections.unmodifiableSet(EXEMPT);
    }

    /** Replaces the whole set, e.g. when loading saved data. Does not notify — this *is* the load. */
    public static void replaceAll(Set<UUID> ids) {
        EXEMPT.clear();
        EXEMPT.addAll(ids);
    }

    /** Snapshot copy, safe to iterate while the set is being modified. */
    public static Set<UUID> copy() {
        return new LinkedHashSet<>(EXEMPT);
    }

    public static void clear() {
        boolean had = !EXEMPT.isEmpty();
        EXEMPT.clear();
        if (had) notifyChanged();
    }

    /**
     * Registered by the persistence layer so a change made by a command is written to the world
     * save. Cleared on server stop together with the set itself.
     */
    public static void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    private static void notifyChanged() {
        Runnable listener = changeListener;
        if (listener != null) listener.run();
    }
}
