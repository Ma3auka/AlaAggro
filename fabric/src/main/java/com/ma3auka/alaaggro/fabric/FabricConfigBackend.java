package com.ma3auka.alaaggro.fabric;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.AggroSettings;
import com.ma3auka.alaaggro.core.ConfigOption;

/**
 * Config storage for Fabric: a JSON file at {@code config/alaaggro.json}.
 *
 * <p>Fabric has no equivalent of NeoForge's config spec, so the file is written and read here. The
 * keys, defaults and ranges still come from the shared {@link ConfigOption} list, which is what
 * keeps the two loaders from drifting apart — an option added once shows up on both.
 *
 * <p>Sections become nested objects, so the file reads much like the NeoForge TOML: values a player
 * looks up in one loader's file are in the same place in the other's.
 */
public final class FabricConfigBackend implements AggroConfig.Backend {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final Map<ConfigOption, Object> values = new LinkedHashMap<>();

    public FabricConfigBackend(Path path) {
        this.path = path;
        for (ConfigOption option : ConfigOption.ALL) {
            values.put(option, option.defaultValue);
        }
    }

    @Override
    public AggroSettings load() {
        readFile();
        return ConfigOption.read(new ConfigOption.Source() {
            @Override
            public boolean bool(ConfigOption option) {
                Object value = values.get(option);
                return value instanceof Boolean flag ? flag : (Boolean) option.defaultValue;
            }

            @Override
            public int integer(ConfigOption option) {
                Object value = values.get(option);
                return value instanceof Number number ? number.intValue() : (Integer) option.defaultValue;
            }

            @Override
            public double number(ConfigOption option) {
                Object value = values.get(option);
                return value instanceof Number number ? number.doubleValue() : (Double) option.defaultValue;
            }

            @Override
            public Set<String> strings(ConfigOption option) {
                Object value = values.get(option);
                Set<String> out = new LinkedHashSet<>();
                if (value instanceof List<?> list) {
                    for (Object entry : list) out.add(String.valueOf(entry));
                }
                return out;
            }
        });
    }

    @Override
    public void set(ConfigOption option, Object value) {
        values.put(option, value);
    }

    @Override
    public void save() {
        try {
            JsonObject root = new JsonObject();
            for (ConfigOption option : ConfigOption.ALL) {
                JsonObject section = root.getAsJsonObject(option.section);
                if (section == null) {
                    section = new JsonObject();
                    root.add(option.section, section);
                }
                section.add(option.key, toJson(values.get(option)));
            }
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException | RuntimeException e) {
            AlaAggro.LOGGER.warn("AlaAggro: could not write {}: {}", path, e.toString());
        }
    }

    /** Reads the file into memory, writing a fresh one with the defaults when it does not exist. */
    private void readFile() {
        if (!Files.isRegularFile(path)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) return;
            JsonObject root = parsed.getAsJsonObject();
            for (ConfigOption option : ConfigOption.ALL) {
                JsonElement sectionElement = root.get(option.section);
                if (sectionElement == null || !sectionElement.isJsonObject()) continue;
                JsonElement value = sectionElement.getAsJsonObject().get(option.key);
                if (value == null) continue;
                values.put(option, fromJson(option, value));
            }
        } catch (IOException | RuntimeException e) {
            // A corrupted file must not stop the server from starting: fall back to what we hold.
            AlaAggro.LOGGER.warn("AlaAggro: could not read {}, using defaults: {}", path, e.toString());
        }
    }

    private static JsonElement toJson(Object value) {
        if (value instanceof Boolean flag) return new com.google.gson.JsonPrimitive(flag);
        if (value instanceof Number number) return new com.google.gson.JsonPrimitive(number);
        JsonArray array = new JsonArray();
        if (value instanceof List<?> list) {
            for (Object entry : list) array.add(String.valueOf(entry));
        }
        return array;
    }

    private static Object fromJson(ConfigOption option, JsonElement element) {
        return switch (option.kind) {
            case BOOL -> element.getAsBoolean();
            case INT -> element.getAsInt();
            case DOUBLE -> element.getAsDouble();
            case STRING_LIST -> {
                List<String> out = new java.util.ArrayList<>();
                if (element.isJsonArray()) {
                    for (JsonElement entry : element.getAsJsonArray()) out.add(entry.getAsString());
                }
                yield out;
            }
        };
    }
}
