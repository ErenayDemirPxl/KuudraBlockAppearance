package dev.kuudraappearance.client.kuudra;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

import java.util.Locale;

/**
 * Client-side Kuudra instance detection.
 *
 * Hypixel exposes "Area: Kuudra" in the player tab list, so tab-list text is
 * the primary signal. The sidebar remains as a fallback in case Hypixel moves
 * the area line again later.
 */
public final class KuudraDetector {
    private static volatile boolean inKuudra;
    private static volatile String detectionSource = "NONE";
    private static int ticksUntilRefresh;

    private KuudraDetector() {}

    public static boolean isInKuudra() {
        return inKuudra;
    }

    public static String detectionSource() {
        return detectionSource;
    }

    public static void tick(Minecraft minecraft) {
        if (--ticksUntilRefresh > 0) return;
        ticksUntilRefresh = 10;

        Detection detection = detect(minecraft);
        boolean changed = detection.detected != inKuudra;
        inKuudra = detection.detected;
        detectionSource = detection.source;

        // Chunk models are cached. Rebuild only when entering/leaving Kuudra.
        if (changed && minecraft.levelRenderer != null) {
            minecraft.levelRenderer.allChanged();
        }
    }

    private static Detection detect(Minecraft minecraft) {
        if (minecraft.level == null) return Detection.none();

        // PRIMARY: Hypixel's tab list includes "Area: Kuudra".
        try {
            ClientPacketListener connection = minecraft.getConnection();
            if (connection != null) {
                for (PlayerInfo info : connection.getListedOnlinePlayers()) {
                    Component display = info.getTabListDisplayName();
                    if (isAreaKuudra(display == null ? null : display.getString())) {
                        return new Detection(true, "TABLIST");
                    }

                }
            }
        } catch (Throwable ignored) {
            // Keep going; sidebar fallback may still work.
        }

        // FALLBACK: scoreboard/sidebar text.
        try {
            Scoreboard scoreboard = minecraft.level.getScoreboard();
            Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebar != null) {
                if (containsKuudra(sidebar.getDisplayName().getString())) {
                    return new Detection(true, "SIDEBAR");
                }
                for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
                    if (entry.isHidden()) continue;
                    if (containsKuudra(entry.owner())) return new Detection(true, "SIDEBAR");
                    if (containsKuudra(entry.ownerName().getString())) return new Detection(true, "SIDEBAR");
                    if (entry.display() != null && containsKuudra(entry.display().getString())) {
                        return new Detection(true, "SIDEBAR");
                    }
                }
            }
        } catch (Throwable ignored) {
            // Detection simply remains off for this refresh.
        }

        return Detection.none();
    }

    private static boolean isAreaKuudra(String text) {
        if (text == null) return false;
        String normalized = normalize(text);
        return normalized.contains("area:kuudra") || normalized.contains("area kuudra");
    }

    private static boolean containsKuudra(String text) {
        return text != null && text.toLowerCase(Locale.ROOT).contains("kuudra");
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\t", "");
    }

    private record Detection(boolean detected, String source) {
        private static Detection none() {
            return new Detection(false, "NONE");
        }
    }
}
