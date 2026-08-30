package com.ma3auka.alaaggro.core;

/**
 * Live settings, shared by every handler, goal and command.
 *
 * <p>Handlers read {@link #get()} on the hot path — it returns an already-built immutable record,
 * so a mob spawning never touches the loader's config machinery. The loader supplies a
 * {@link Backend} at startup; from then on the mod neither knows nor cares whether the values came
 * from a NeoForge {@code ModConfigSpec} or a Fabric JSON file.
 *
 * <p>{@link #generation()} counts reloads. Injected mobs remember the generation they were built
 * under, so after a config change the periodic scan can rebuild exactly the mobs that are out of
 * date, without a bookkeeping list of every mob in the world.
 */
public final class AggroConfig {

    /** Storage, implemented once per loader. */
    public interface Backend {
        /** Reads current values. Called on bind, on reload, and after every {@link #set}. */
        AggroSettings load();

        /** Updates one value in memory. */
        void set(ConfigOption option, Object value);

        /** Flushes in-memory values to disk, so an in-game change survives a restart. */
        void save();
    }

    private static volatile AggroSettings current = AggroSettings.defaults();
    private static volatile int generation = 1;
    private static volatile Backend backend;

    private AggroConfig() {}

    /** Called once per loader during mod construction. */
    public static void bind(Backend newBackend) {
        backend = newBackend;
        reload();
    }

    public static AggroSettings get() {
        return current;
    }

    public static int generation() {
        return generation;
    }

    /** Re-reads values from the backend and bumps the generation. */
    public static void reload() {
        Backend b = backend;
        AggroSettings loaded = b == null ? AggroSettings.defaults() : b.load();
        current = loaded == null ? AggroSettings.defaults() : loaded;
        generation++;
    }

    /**
     * Changes one option, writes it to disk and refreshes the live values — the path used by
     * {@code /alaaggro set …} and {@code /alaaggro toggle}. Persisting here rather than in the
     * command is what keeps an in-game change from being lost on the next world load.
     */
    public static void set(ConfigOption option, Object value) {
        Backend b = backend;
        if (b == null) return;
        b.set(option, value);
        b.save();
        reload();
    }

    /** Test hook: drop the backend and return to declared defaults. */
    public static void resetForTesting() {
        backend = null;
        current = AggroSettings.defaults();
        generation++;
    }
}
