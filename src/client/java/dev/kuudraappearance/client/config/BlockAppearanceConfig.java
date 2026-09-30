package dev.kuudraappearance.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BlockAppearanceConfig {
    /** Tint was removed in v0.4.0. Gson ignores the old tintArgb field automatically. */
    public record Rule(String replacement) {}

    private static final class Data {
        Map<String, Rule> rules = new LinkedHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("kuudraappearance.json");
    private static final Map<String, Rule> RULES = new LinkedHashMap<>();

    private BlockAppearanceConfig() {}

    public static void load() {
        RULES.clear();
        if (!Files.exists(FILE)) return;
        try {
            String json = Files.readString(FILE);
            JsonElement root = GSON.fromJson(json, JsonElement.class);
            if (root != null && root.isJsonObject() && root.getAsJsonObject().has("rules")) {
                Data loaded = GSON.fromJson(root, Data.class);
                if (loaded != null && loaded.rules != null) RULES.putAll(loaded.rules);
            } else if (root != null && root.isJsonObject()) {
                Type legacyType = new TypeToken<Map<String, Rule>>() {}.getType();
                Map<String, Rule> legacy = GSON.fromJson(root, legacyType);
                if (legacy != null) RULES.putAll(legacy);
            }
        } catch (Exception e) {
            System.err.println("[KuudraAppearance] Could not load config: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data data = new Data();
            data.rules.putAll(RULES);
            Files.writeString(FILE, GSON.toJson(data));
        } catch (IOException e) {
            System.err.println("[KuudraAppearance] Could not save config: " + e.getMessage());
        }
    }

    public static Rule get(String sourceId) { return RULES.get(sourceId); }
    public static Map<String, Rule> rules() { return Collections.unmodifiableMap(RULES); }

    public static void put(String sourceId, String replacementId) {
        RULES.put(sourceId, new Rule(replacementId));
        save();
    }

    public static void replaceAll(Map<String, Rule> rules) {
        RULES.clear();
        if (rules != null) RULES.putAll(rules);
        save();
    }

    public static Map<String, Rule> snapshot() {
        return new LinkedHashMap<>(RULES);
    }

    public static void remove(String sourceId) {
        RULES.remove(sourceId);
        save();
    }

    public static void clear() {
        RULES.clear();
        save();
    }
}
