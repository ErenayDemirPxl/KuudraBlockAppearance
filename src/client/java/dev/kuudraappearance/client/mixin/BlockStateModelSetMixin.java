package dev.kuudraappearance.client.mixin;

import dev.kuudraappearance.client.BlockAppearanceManager;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockStateModelSet.class)
public abstract class BlockStateModelSetMixin {
    @ModifyVariable(method = "get", at = @At("HEAD"), argsOnly = true)
    private BlockState kuudraappearance$replaceModelLookupState(BlockState original) {
        return BlockAppearanceManager.replacementFor(original);
    }
}
