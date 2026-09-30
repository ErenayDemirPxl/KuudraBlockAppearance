package dev.kuudraappearance.client;

import dev.kuudraappearance.client.config.BlockAppearanceConfig;
import dev.kuudraappearance.client.kuudra.KuudraDetector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BlockAppearanceManager {
    private static final Set<String> OBSERVED_KUUDRA_BLOCKS = Collections.synchronizedSet(new LinkedHashSet<>());

    private BlockAppearanceManager() {}

    public static BlockState replacementFor(BlockState original) {
        String sourceId = sourceId(original);
        if (sourceId == null) return original;
        observeIfNeeded(sourceId, original);
        if (!KuudraDetector.isInKuudra()) return original;

        BlockAppearanceConfig.Rule rule = BlockAppearanceConfig.get(sourceId);
        if (rule == null || rule.replacement() == null || rule.replacement().isBlank()) return original;

        try {
            Identifier replacementKey = Identifier.parse(rule.replacement());
            if (!BuiltInRegistries.BLOCK.containsKey(replacementKey)) return original;
            Block replacement = BuiltInRegistries.BLOCK.getValue(replacementKey);
            if (replacement == null) return original;
            return preserveShapeState(original, replacement.defaultBlockState());
        } catch (Exception ignored) {
            return original;
        }
    }

    /** Preserve stair direction/half/shape and slab position when replacing like with like. */
    private static BlockState preserveShapeState(BlockState original, BlockState target) {
        try {
            if (original.getBlock() instanceof StairBlock && target.getBlock() instanceof StairBlock) {
                target = target.setValue(StairBlock.FACING, original.getValue(StairBlock.FACING));
                target = target.setValue(StairBlock.HALF, original.getValue(StairBlock.HALF));
                target = target.setValue(StairBlock.SHAPE, original.getValue(StairBlock.SHAPE));
            } else if (original.getBlock() instanceof SlabBlock && target.getBlock() instanceof SlabBlock) {
                target = target.setValue(SlabBlock.TYPE, original.getValue(SlabBlock.TYPE));
            }
        } catch (Exception ignored) {
            // If Mojang adds/removes a state property later, rendering still falls back to the target default.
        }
        return target;
    }

    private static String sourceId(BlockState original) {
        Identifier sourceKey = BuiltInRegistries.BLOCK.getKey(original.getBlock());
        return sourceKey == null ? null : sourceKey.toString();
    }

    private static void observeIfNeeded(String sourceId, BlockState original) {
        if (KuudraDetector.isInKuudra() && BlockCatalog.isEligible(sourceId, original)) OBSERVED_KUUDRA_BLOCKS.add(sourceId);
    }

    public static List<String> observedKuudraBlocks() {
        synchronized (OBSERVED_KUUDRA_BLOCKS) {
            List<String> list = new ArrayList<>(OBSERVED_KUUDRA_BLOCKS);
            Collections.sort(list);
            return list;
        }
    }

    public static void clearObservedKuudraBlocks() { OBSERVED_KUUDRA_BLOCKS.clear(); }

    public static boolean isValidSourceId(String value) {
        try {
            Identifier id = Identifier.parse(normalize(value));
            return BuiltInRegistries.BLOCK.containsKey(id);
        } catch (Exception e) { return false; }
    }

    public static boolean isValidAppearanceId(String value) {
        return isValidSourceId(normalize(value));
    }

    public static String normalize(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        return v.contains(":") ? v : "minecraft:" + v;
    }
}
