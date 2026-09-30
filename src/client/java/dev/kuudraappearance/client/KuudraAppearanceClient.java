package dev.kuudraappearance.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kuudraappearance.client.config.BlockAppearanceConfig;
import dev.kuudraappearance.client.gui.BlockAppearanceScreen;
import dev.kuudraappearance.client.kuudra.KuudraDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class KuudraAppearanceClient implements ClientModInitializer {
    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        BlockAppearanceConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("kuudraappearance", "general"));
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.kuudraappearance.open", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
            dispatcher.register(ClientCommands.literal("kba").executes(context -> {
                var client = context.getSource().getClient();
                client.setScreen(new BlockAppearanceScreen(client.screen));
                return 1;
            }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KuudraDetector.tick(client);
            while (openMenuKey.consumeClick()) client.setScreen(new BlockAppearanceScreen(client.screen));
        });
    }

    public static KeyMapping openMenuKey() {
        return openMenuKey;
    }
}
