package dev.kuudraappearance.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Full cubes plus all vanilla slabs/stairs and Soul Sand. Decorative non-block-like entries remain excluded. */
public final class BlockCatalog {
    private BlockCatalog() {}

    public static List<String> allSourceBlockIds() {
        List<String> result = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null && isEligible(id.toString(), block.defaultBlockState())) result.add(id.toString());
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    public static List<String> allAppearanceIds() {
        return allSourceBlockIds();
    }

    public static boolean isEligible(String id, BlockState state) {
        if (id == null || state == null || state.isAir()) return false;
        if ("minecraft:soul_sand".equals(id)) return true;
        if (state.getBlock() instanceof StairBlock || state.getBlock() instanceof SlabBlock) return true;
        if (state.getRenderShape() != RenderShape.MODEL) return false;
        try {
            return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (Throwable ignored) {
            return state.isSolidRender();
        }
    }

    public static String displayName(String id) {
        if (id == null) return "";
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        String[] words = path.split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
