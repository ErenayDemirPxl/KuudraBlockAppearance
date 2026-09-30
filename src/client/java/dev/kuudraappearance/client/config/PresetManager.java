package dev.kuudraappearance.client.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PresetManager {
    public record Preset(String name, String description, Map<String, BlockAppearanceConfig.Rule> rules) {}

    private PresetManager() {}

    public static List<Preset> builtIns() {
        List<Preset> out = new ArrayList<>();
        out.add(new Preset("Vanilla / Clear", "Removes all KBA replacements.", Map.of()));

        Map<String, BlockAppearanceConfig.Rule> mossy = new LinkedHashMap<>();
        mossy.put("minecraft:andesite", new BlockAppearanceConfig.Rule("minecraft:cobbled_deepslate"));
        mossy.put("minecraft:bedrock", new BlockAppearanceConfig.Rule("minecraft:green_wool"));
        mossy.put("minecraft:coal_block", new BlockAppearanceConfig.Rule("minecraft:brown_terracotta"));
        mossy.put("minecraft:cobblestone", new BlockAppearanceConfig.Rule("minecraft:green_concrete_powder"));
        mossy.put("minecraft:gray_wool", new BlockAppearanceConfig.Rule("minecraft:lime_terracotta"));
        mossy.put("minecraft:nether_bricks", new BlockAppearanceConfig.Rule("minecraft:cobbled_deepslate"));
        mossy.put("minecraft:stone", new BlockAppearanceConfig.Rule("minecraft:deepslate"));
        mossy.put("minecraft:cobblestone_stairs", new BlockAppearanceConfig.Rule("minecraft:cobbled_deepslate_stairs"));
        mossy.put("minecraft:cobblestone_slab", new BlockAppearanceConfig.Rule("minecraft:cobbled_deepslate_slab"));
        out.add(new Preset("Mossy Preset", "Mossy Kuudra setup. Load it, then edit any mapping you want.", mossy));

        Map<String, BlockAppearanceConfig.Rule> bloody = new LinkedHashMap<>();
        bloody.put("minecraft:andesite", new BlockAppearanceConfig.Rule("minecraft:red_nether_bricks"));
        bloody.put("minecraft:bedrock", new BlockAppearanceConfig.Rule("minecraft:crimson_nylium"));
        bloody.put("minecraft:coal_block", new BlockAppearanceConfig.Rule("minecraft:nether_wart_block"));
        bloody.put("minecraft:cobblestone", new BlockAppearanceConfig.Rule("minecraft:red_concrete_powder"));
        bloody.put("minecraft:cobblestone_slab", new BlockAppearanceConfig.Rule("minecraft:red_nether_brick_slab"));
        bloody.put("minecraft:cobblestone_stairs", new BlockAppearanceConfig.Rule("minecraft:red_nether_brick_stairs"));
        bloody.put("minecraft:cyan_terracotta", new BlockAppearanceConfig.Rule("minecraft:red_concrete"));
        bloody.put("minecraft:gray_wool", new BlockAppearanceConfig.Rule("minecraft:red_wool"));
        bloody.put("minecraft:nether_bricks", new BlockAppearanceConfig.Rule("minecraft:red_nether_bricks"));
        bloody.put("minecraft:stone", new BlockAppearanceConfig.Rule("minecraft:netherrack"));
        out.add(new Preset("Bloody Kuudra", "Red-themed Kuudra setup. Load it, then edit any mapping you want.", bloody));
        return out;
    }
}
