package dev.kuudraappearance.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SaveManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("kuudraappearance").resolve("saves");
    private static final Path EXPORT_DIR = FabricLoader.getInstance().getConfigDir().resolve("kuudraappearance").resolve("exports");
    private static final Type RULE_MAP = new TypeToken<Map<String, BlockAppearanceConfig.Rule>>() {}.getType();

    private SaveManager() {}

    public static List<String> names() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(DIR);
            try (var stream = Files.list(DIR)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .forEach(p -> out.add(p.getFileName().toString().replaceFirst("\\.json$", "")));
            }
        } catch (IOException ignored) {}
        out.sort(Comparator.naturalOrder());
        return out;
    }

    public static void save(String name) throws IOException {
        String clean = cleanName(name);
        if (clean.isBlank()) throw new IOException("Enter a save name");
        Files.createDirectories(DIR);
        Files.writeString(DIR.resolve(clean + ".json"), GSON.toJson(BlockAppearanceConfig.snapshot()));
    }

    public static Map<String, BlockAppearanceConfig.Rule> load(String name) throws IOException {
        Path file = DIR.resolve(cleanName(name) + ".json");
        if (!Files.exists(file)) throw new IOException("Save not found");
        return readRules(file);
    }

    public static void delete(String name) throws IOException {
        Files.deleteIfExists(DIR.resolve(cleanName(name) + ".json"));
    }

    public static void rename(String oldName, String newName) throws IOException {
        String oldClean = cleanName(oldName);
        String newClean = cleanName(newName);
        if (newClean.isBlank()) throw new IOException("Enter a new name first");
        Path src = DIR.resolve(oldClean + ".json");
        Path dst = DIR.resolve(newClean + ".json");
        if (!Files.exists(src)) throw new IOException("Save not found");
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Export a user save as a shareable JSON file. */
    public static Path exportSave(String name) throws IOException {
        String clean = cleanName(name);
        Path src = DIR.resolve(clean + ".json");
        if (!Files.exists(src)) throw new IOException("Save not found");
        Files.createDirectories(EXPORT_DIR);
        Path dst = EXPORT_DIR.resolve(clean + ".json");
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        return dst;
    }

    /**
     * Import every valid JSON file currently in config/kuudraappearance/exports.
     * Existing saves with the same name are replaced so shared files are easy to update.
     */
    public static int importExports() throws IOException {
        Files.createDirectories(DIR);
        Files.createDirectories(EXPORT_DIR);
        int imported = 0;
        try (var stream = Files.list(EXPORT_DIR)) {
            for (Path src : stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json")).toList()) {
                // Parse before copying so a random/broken JSON file cannot become a save.
                readRules(src);
                String fileName = src.getFileName().toString();
                String rawName = fileName.substring(0, fileName.length() - 5);
                String clean = cleanName(rawName);
                if (clean.isBlank()) continue;
                Files.copy(src, DIR.resolve(clean + ".json"), StandardCopyOption.REPLACE_EXISTING);
                imported++;
            }
        }
        return imported;
    }

    public static Path exportDirectory() throws IOException {
        Files.createDirectories(EXPORT_DIR);
        return EXPORT_DIR;
    }

    private static Map<String, BlockAppearanceConfig.Rule> readRules(Path file) throws IOException {
        try {
            Map<String, BlockAppearanceConfig.Rule> data = GSON.fromJson(Files.readString(file), RULE_MAP);
            return data == null ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
        } catch (RuntimeException e) {
            throw new IOException("Invalid KBA save JSON: " + file.getFileName(), e);
        }
    }

    private static String cleanName(String value) {
        String v = value == null ? "" : value.trim();
        v = v.replaceAll("[^A-Za-z0-9 _.-]", "_");
        if (v.length() > 48) v = v.substring(0, 48);
        return v;
    }
}
