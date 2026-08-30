package com.ma3auka.alaaggro.neoforge;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.AggroSettings;
import com.ma3auka.alaaggro.core.ConfigOption;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config storage for NeoForge, built from {@link ConfigOption} rather than written out by hand.
 *
 * <p>The spec is generated from the shared option list, so the TOML file keeps its existing name,
 * sections and keys — an upgrading player's {@code alaaggro-server.toml} is read as before — and the
 * in-game Mods → AlaAggro → Settings screen keeps working, because NeoForge builds that screen from
 * this spec. Adding an option to the shared list is enough; nothing here needs editing.
 */
public final class NeoForgeConfigBackend implements AggroConfig.Backend {

    private static final Map<ConfigOption, ModConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();
    public static final ModConfigSpec SPEC = build();

    @SuppressWarnings("unchecked")
    private static ModConfigSpec build() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        String openSection = null;
        for (ConfigOption option : ConfigOption.ALL) {
            if (!option.section.equals(openSection)) {
                if (openSection != null) builder.pop();
                builder.push(option.section);
                openSection = option.section;
            }
            builder.comment(option.comment);
            // Unboxed on purpose. Passing a Boolean/Integer/Double picks the generic overload,
            // which hands back a plain ConfigValue instead of the typed one — and that only shows
            // up at runtime, as a ClassCastException while the server is starting.
            ModConfigSpec.ConfigValue<?> value = switch (option.kind) {
                case BOOL -> builder.define(option.key, ((Boolean) option.defaultValue).booleanValue());
                case INT -> builder.defineInRange(option.key, ((Integer) option.defaultValue).intValue(),
                        (int) option.min, (int) option.max);
                case DOUBLE -> builder.defineInRange(option.key, ((Double) option.defaultValue).doubleValue(),
                        option.min, option.max);
                case STRING_LIST -> builder.defineListAllowEmpty(option.key, List.of(),
                        () -> option.listExample, entry -> entry instanceof String);
            };
            VALUES.put(option, value);
        }
        if (openSection != null) builder.pop();
        return builder.build();
    }

    @Override
    public AggroSettings load() {
        if (!SPEC.isLoaded()) return AggroSettings.defaults();
        // Read by the shape of the stored value rather than by casting to a specific config-value
        // class: which class the builder returns depends on overload resolution, and a mismatch
        // there is otherwise a crash at server start instead of a compile error.
        return ConfigOption.read(new ConfigOption.Source() {
            @Override
            public boolean bool(ConfigOption option) {
                Object raw = value(option);
                return raw instanceof Boolean flag ? flag : (Boolean) option.defaultValue;
            }

            @Override
            public int integer(ConfigOption option) {
                Object raw = value(option);
                return raw instanceof Number number ? number.intValue() : (Integer) option.defaultValue;
            }

            @Override
            public double number(ConfigOption option) {
                Object raw = value(option);
                return raw instanceof Number number ? number.doubleValue() : (Double) option.defaultValue;
            }

            @Override
            public Set<String> strings(ConfigOption option) {
                Object raw = value(option);
                Set<String> out = new HashSet<>();
                if (raw instanceof List<?> list) {
                    for (Object entry : list) out.add(String.valueOf(entry));
                }
                return out;
            }

            private Object value(ConfigOption option) {
                ModConfigSpec.ConfigValue<?> config = VALUES.get(option);
                return config == null ? null : config.get();
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(ConfigOption option, Object value) {
        ModConfigSpec.ConfigValue<Object> config = (ModConfigSpec.ConfigValue<Object>) VALUES.get(option);
        if (config == null) return;
        config.set(value);
    }

    /**
     * Writes the file. Without this an in-game {@code /alaaggro toggle} only lived until the world
     * reloaded, and the operator had to run the command again after every restart.
     */
    @Override
    public void save() {
        try {
            if (SPEC.isLoaded()) SPEC.save();
        } catch (Throwable t) {
            AlaAggro.LOGGER.warn("AlaAggro: could not write the config file: {}", t.toString());
        }
    }
}
