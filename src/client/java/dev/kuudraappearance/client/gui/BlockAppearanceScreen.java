package dev.kuudraappearance.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kuudraappearance.client.BlockAppearanceManager;
import dev.kuudraappearance.client.BlockCatalog;
import dev.kuudraappearance.client.KuudraAppearanceClient;
import dev.kuudraappearance.client.config.BlockAppearanceConfig;
import dev.kuudraappearance.client.config.PresetManager;
import dev.kuudraappearance.client.config.SaveManager;
import dev.kuudraappearance.client.kuudra.KuudraDetector;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main KBA screen. This screen intentionally owns its visual styling rather than
 * using vanilla button skins so the whole mod has one consistent compact UI.
 */
public final class BlockAppearanceScreen extends Screen {
    private enum Tab { EDITOR, CHANGED, KUUDRA_BLOCKS, PRESETS, SAVES, CONFIG }

    // KBA palette (ARGB)
    private static final int BG = 0xE60D0507;
    private static final int PANEL = 0xFF1A0A10;
    private static final int PANEL_BORDER = 0xFF4A1626;
    private static final int PANEL_TOP_ACCENT = 0xFFE0283C;
    private static final int SOURCE_ACCENT = 0xFFE0283C;
    private static final int APPEARANCE_ACCENT = 0xFFC2187A;
    private static final int TEXT = 0xFFF5ECEF;
    private static final int MUTED = 0xFFC3A7B1;
    private static final int DANGER = 0xFF8A1F3A;
    private static final int HOVER = 0x0DFFFFFF;
    private static final int SELECTED = 0x26E0283C;
    private static final int SUCCESS = 0xFF69D38B;
    private static final int DARK_TEXT = 0xFFF5ECEF;

    private static final int OUTER = 16;
    private static final int GAP = 12;
    private static final int ROW_H = 20;
    private static final int BODY_TOP = 66;
    private static final int FOOTER_H = 26;

    private final Screen parent;
    private Tab tab = Tab.EDITOR;
    private EditBox search;
    private EditBox source;
    private EditBox replacement;
    private EditBox saveName;
    private String status = "Choose a block and customize its appearance";
    private String toastMessage = "";
    private long toastUntil;
    private long resetAllArmedUntil;
    private boolean listeningForKey;
    private boolean listSetsReplacement;
    private String searchText = "";
    private String editorSourceText = "";
    private String editorReplacementText = "";
    private String saveNameText = "";
    private double editorScroll;
    private double changedScroll;
    private double kuudraScroll;
    private double saveScroll;
    private final List<String> sourceBlocks = BlockCatalog.allSourceBlockIds();
    private final List<String> appearanceBlocks = BlockCatalog.allAppearanceIds();

    public BlockAppearanceScreen(Screen parent) {
        super(Component.literal("Kuudra Block Appearance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        captureValues();
        clearWidgets();
        switch (tab) {
            case EDITOR -> buildEditor();
            case CHANGED -> buildChanged();
            case KUUDRA_BLOCKS -> buildKuudraBlocks();
            case PRESETS -> buildPresets();
            case SAVES -> buildSaves();
            case CONFIG -> buildConfig();
        }
    }

    private void captureValues() {
        if (source != null) editorSourceText = source.getValue();
        if (replacement != null) editorReplacementText = replacement.getValue();
        if (search != null) searchText = search.getValue();
        if (saveName != null) saveNameText = saveName.getValue();
    }

    private void setTab(Tab next) {
        captureValues();
        tab = next;
        listeningForKey = false;
        clearStaleRefs();
        init();
    }

    private int bodyBottom() { return Math.max(BODY_TOP + 120, this.height - FOOTER_H - 8); }
    private int bodyHeight() { return bodyBottom() - BODY_TOP; }
    private int usableWidth() { return Math.max(260, this.width - OUTER * 2); }
    private int editorLeftWidth() { return Math.max(180, (usableWidth() - GAP) * 56 / 100); }
    private int editorRightX() { return OUTER + editorLeftWidth() + GAP; }
    private int editorRightWidth() { return Math.max(150, usableWidth() - GAP - editorLeftWidth()); }
    private int editorListY() { return BODY_TOP + 86; }
    private int editorListHeight() { return Math.max(48, bodyBottom() - editorListY() - 12); }

    private void buildEditor() {
        int leftX = OUTER;
        int leftW = editorLeftWidth();
        int rightX = editorRightX();
        int rightW = editorRightWidth();

        search = new EditBox(this.font, leftX + 30, BODY_TOP + 31, Math.max(80, leftW - 42), 18, Component.literal("Search blocks"));
        search.setMaxLength(128);
        search.setHint(Component.literal("Search blocks..."));
        search.setValue(searchText);
        search.setResponder(v -> { searchText = v; editorScroll = 0; });
        addRenderableWidget(search);

        source = new EditBox(this.font, rightX + 12, BODY_TOP + 34, Math.max(70, rightW - 24), 20, Component.literal("Source block"));
        source.setMaxLength(128);
        source.setHint(Component.literal("minecraft:cyan_terracotta"));
        source.setValue(editorSourceText);
        source.setResponder(v -> editorSourceText = v);
        addRenderableWidget(source);

        replacement = new EditBox(this.font, rightX + 12, BODY_TOP + 82, Math.max(70, rightW - 24), 20, Component.literal("Appearance"));
        replacement.setMaxLength(128);
        replacement.setHint(Component.literal("minecraft:moss_block"));
        replacement.setValue(editorReplacementText);
        replacement.setResponder(v -> editorReplacementText = v);
        addRenderableWidget(replacement);

        editorScroll = clampScroll(editorScroll, filteredBlocks(searchText).size(), editorListHeight());
    }

    private List<String> filteredBlocks(String query) {
        List<String> base = listSetsReplacement ? appearanceBlocks : sourceBlocks;
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return base;
        List<String> out = new ArrayList<>();
        for (String id : base) {
            if (id.contains(q) || BlockCatalog.displayName(id).toLowerCase(Locale.ROOT).contains(q)) out.add(id);
        }
        return out;
    }

    private void buildChanged() {
        BlockAppearanceConfig.load();
        changedScroll = clampScroll(changedScroll, sortedRules().size(), genericListHeight());
    }

    private void buildKuudraBlocks() {
        kuudraScroll = clampScroll(kuudraScroll, availableKuudraBlocks().size(), genericListHeight());
    }

    private void buildPresets() {
        // Built-ins are immutable. Applying one copies it into the editable config.
    }

    private void buildSaves() {
        int panelX = OUTER;
        int panelW = usableWidth();
        saveName = new EditBox(this.font, panelX + 12, BODY_TOP + 34, Math.min(260, Math.max(100, panelW - 250)), 20, Component.literal("Save name"));
        saveName.setMaxLength(96);
        saveName.setHint(Component.literal("My Kuudra setup"));
        saveName.setValue(saveNameText);
        saveName.setResponder(v -> saveNameText = v);
        addRenderableWidget(saveName);
        saveScroll = clampScroll(saveScroll, SaveManager.names().size(), savesListHeight());
    }

    private void buildConfig() {
        // Config buttons are drawn/handled manually to keep KBA styling consistent.
    }

    private List<Map.Entry<String, BlockAppearanceConfig.Rule>> sortedRules() {
        List<Map.Entry<String, BlockAppearanceConfig.Rule>> rules = new ArrayList<>(BlockAppearanceConfig.rules().entrySet());
        rules.sort(Map.Entry.comparingByKey());
        return rules;
    }

    private List<String> availableKuudraBlocks() {
        List<String> out = new ArrayList<>();
        for (String id : BlockAppearanceManager.observedKuudraBlocks()) {
            if (!BlockAppearanceConfig.rules().containsKey(id)) out.add(id);
        }
        return out;
    }

    private void openEditorForSource(String sourceId) {
        editorSourceText = sourceId;
        editorReplacementText = "";
        clearStaleRefs();
        tab = Tab.EDITOR;
        listeningForKey = false;
        init();
    }

    private void openEditorForRule(String sourceId, BlockAppearanceConfig.Rule rule) {
        editorSourceText = sourceId;
        editorReplacementText = rule.replacement();
        clearStaleRefs();
        tab = Tab.EDITOR;
        listeningForKey = false;
        init();
    }

    private void clearStaleRefs() {
        source = null;
        replacement = null;
        search = null;
        saveName = null;
    }

    private void showToast(String message) {
        toastMessage = message;
        toastUntil = System.currentTimeMillis() + 2000L;
    }

    private void apply() {
        String from = BlockAppearanceManager.normalize(source == null ? editorSourceText : source.getValue());
        String to = BlockAppearanceManager.normalize(replacement == null ? editorReplacementText : replacement.getValue());
        if (!BlockAppearanceManager.isValidSourceId(from)) { status = "Invalid source block"; return; }
        if (!BlockAppearanceManager.isValidAppearanceId(to)) { status = "Invalid appearance block"; return; }
        BlockAppearanceConfig.put(from, to);
        editorSourceText = from;
        editorReplacementText = to;
        if (source != null) source.setValue(from);
        if (replacement != null) replacement.setValue(to);
        status = "Applied " + BlockCatalog.displayName(from) + " -> " + BlockCatalog.displayName(to);
        showToast("Mapping applied");
        refreshWorld();
    }

    private void reset() {
        String from = BlockAppearanceManager.normalize(source == null ? editorSourceText : source.getValue());
        BlockAppearanceConfig.remove(from);
        status = "Reset " + BlockCatalog.displayName(from);
        refreshWorld();
    }

    private void resetAll() {
        long now = System.currentTimeMillis();
        if (now > resetAllArmedUntil) {
            resetAllArmedUntil = now + 3000L;
            status = "Click Reset all again to confirm";
            return;
        }
        resetAllArmedUntil = 0;
        BlockAppearanceConfig.clear();
        status = "All replacements cleared";
        refreshWorld();
    }

    private void saveCurrent() {
        try {
            String name = saveName == null ? saveNameText : saveName.getValue();
            SaveManager.save(name);
            saveNameText = name;
            status = "Saved current setup as " + saveNameText;
            showToast("Save created");
            init();
        } catch (Exception e) { status = e.getMessage(); }
    }

    private void applyPreset(PresetManager.Preset preset) {
        BlockAppearanceConfig.replaceAll(preset.rules());
        status = "Loaded preset: " + preset.name() + " (still editable)";
        showToast("Preset loaded");
        refreshWorld();
    }

    private void loadSave(String name) {
        try {
            BlockAppearanceConfig.replaceAll(SaveManager.load(name));
            status = "Loaded save: " + name;
            showToast("Save loaded");
            refreshWorld();
        } catch (Exception e) { status = e.getMessage(); }
    }

    private void deleteSave(String name) {
        try {
            SaveManager.delete(name);
            status = "Deleted save: " + name;
            init();
        } catch (Exception e) { status = e.getMessage(); }
    }

    private void renameSave(String name) {
        try {
            String newName = saveName == null ? saveNameText : saveName.getValue();
            SaveManager.rename(name, newName);
            saveNameText = newName;
            status = "Renamed " + name + " to " + newName;
            init();
        } catch (Exception e) { status = e.getMessage(); }
    }

    private void exportSave(String name) {
        try {
            var path = SaveManager.exportSave(name);
            status = "Exported " + name + " to " + path;
            showToast("Save exported");
        } catch (Exception e) { status = e.getMessage(); }
    }

    private void importSaves() {
        try {
            int count = SaveManager.importExports();
            var path = SaveManager.exportDirectory();
            status = count > 0
                    ? "Imported " + count + " save" + (count == 1 ? "" : "s") + " from " + path
                    : "No JSONs found. Put shared saves in " + path;
            if (count > 0) showToast("Imported " + count + " save" + (count == 1 ? "" : "s"));
            init();
        } catch (Exception e) { status = e.getMessage(); }
    }

    private void refreshWorld() {
        if (minecraft.levelRenderer != null) minecraft.levelRenderer.allChanged();
    }

    private ItemStack blockIcon(String id) {
        if (id == null || id.isBlank()) return ItemStack.EMPTY;
        try {
            String actual = BlockAppearanceManager.normalize(id);
            Identifier key = Identifier.parse(actual);
            if (!BuiltInRegistries.BLOCK.containsKey(key)) return ItemStack.EMPTY;
            Block block = BuiltInRegistries.BLOCK.getValue(key);
            if (block == null) return ItemStack.EMPTY;
            return new ItemStack(block.asItem());
        } catch (Exception ignored) { return ItemStack.EMPTY; }
    }

    private double clampScroll(double scroll, int itemCount, int viewportHeight) {
        double max = Math.max(0, itemCount * ROW_H - viewportHeight);
        return Math.max(0, Math.min(scroll, max));
    }

    private int firstVisibleRow(double scroll) { return Math.max(0, (int) (scroll / ROW_H)); }
    private int rowYOffset(double scroll) { return -((int) scroll % ROW_H); }
    private boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private int genericListY() { return BODY_TOP + 36; }
    private int genericListHeight() { return Math.max(50, bodyBottom() - genericListY() - 12); }
    private int savesListY() { return BODY_TOP + 70; }
    private int savesListHeight() { return Math.max(50, bodyBottom() - savesListY() - 12); }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        outline(g, x, y, w, h, PANEL_BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, PANEL_TOP_ACCENT);
    }

    private void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void centeredShadow(GuiGraphicsExtractor g, String text, int centerX, int y, int color) {
        g.text(this.font, text, centerX - this.font.width(text) / 2, y, color, true);
    }

    private void renderButton(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int y, int w, int h, String label, int fill, int border, int textColor) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? lighten(fill) : fill);
        outline(g, x, y, w, h, border);
        centeredShadow(g, label, x + w / 2, y + (h - 8) / 2, textColor);
    }

    private int lighten(int color) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.min(255, ((color >>> 16) & 0xFF) + 14);
        int gr = Math.min(255, ((color >>> 8) & 0xFF) + 14);
        int b = Math.min(255, (color & 0xFF) + 14);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private void renderHeaderAndTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, "Kuudra Mod Editor", OUTER, 15, TEXT, true);

        String pill = KuudraDetector.isInKuudra() ? "KUUDRA ACTIVE" : "OUTSIDE KUUDRA";
        int pillW = this.font.width(pill) + 16;
        int pillX = this.width - OUTER - pillW;
        int pillColor = KuudraDetector.isInKuudra() ? SUCCESS : SOURCE_ACCENT;
        g.fill(pillX, 10, pillX + pillW, 29, PANEL);
        outline(g, pillX, 10, pillW, 19, pillColor);
        centeredShadow(g, pill, pillX + pillW / 2, 16, pillColor);

        String[] labels = {"Editor", "Changed", "Kuudra Blocks", "Presets", "Saves", "Config"};
        int x = OUTER;
        int y = 40;
        int spacing = 18;
        for (int i = 0; i < labels.length; i++) {
            int w = this.font.width(labels[i]);
            boolean active = tab.ordinal() == i;
            int color = active ? TEXT : MUTED;
            if (active) g.fill(x - 4, y - 3, x + w + 4, y + 16, 0x22E0283C);
            g.text(this.font, labels[i], x, y, color, true);
            if (active) g.fill(x, y + 13, x + w, y + 16, SOURCE_ACCENT);
            x += w + spacing;
        }
    }

    private Tab tabAt(double mx, double my) {
        if (my < 37 || my >= 58) return null;
        String[] labels = {"Editor", "Changed", "Kuudra Blocks", "Presets", "Saves", "Config"};
        int x = OUTER;
        int spacing = 18;
        for (int i = 0; i < labels.length; i++) {
            int w = this.font.width(labels[i]);
            if (mx >= x - 3 && mx < x + w + 3) return Tab.values()[i];
            x += w + spacing;
        }
        return null;
    }

    private void renderFooter(GuiGraphicsExtractor g) {
        int y = this.height - FOOTER_H;
        g.fill(OUTER, y, this.width - OUTER, y + 1, PANEL_BORDER);
        String scope = KuudraDetector.isInKuudra()
                ? "Kuudra detected via " + KuudraDetector.detectionSource() + " — replacements active"
                : "Outside Kuudra — replacements disabled";
        g.centeredText(this.font, scope, this.width / 2, y + 9, KuudraDetector.isInKuudra() ? MUTED : SOURCE_ACCENT);
    }

    private void renderScrollBar(GuiGraphicsExtractor g, int x, int y, int h, int itemCount, double scroll) {
        int content = itemCount * ROW_H;
        if (content <= h || h <= 0) return;
        g.fill(x, y, x + 2, y + h, 0x442C2433);
        int thumbH = Math.max(14, h * h / content);
        int maxTravel = h - thumbH;
        int maxScroll = content - h;
        int thumbY = y + (int) Math.round((scroll / maxScroll) * maxTravel);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, MUTED);
    }

    private void renderEditorBackground(GuiGraphicsExtractor g) {
        panel(g, OUTER, BODY_TOP, editorLeftWidth(), bodyHeight());
        panel(g, editorRightX(), BODY_TOP, editorRightWidth(), bodyHeight());
        // Field accent borders sit underneath the vanilla text fields.
        outline(g, editorRightX() + 10, BODY_TOP + 32, editorRightWidth() - 20, 24, SOURCE_ACCENT);
        outline(g, editorRightX() + 10, BODY_TOP + 80, editorRightWidth() - 20, 24, APPEARANCE_ACCENT);
    }

    private void renderEditorForeground(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int leftX = OUTER, leftW = editorLeftWidth();
        int rightX = editorRightX(), rightW = editorRightWidth();
        g.text(this.font, "Blocks", leftX + 12, BODY_TOP + 11, TEXT, true);
        g.text(this.font, "⌕", leftX + 14, BODY_TOP + 36, MUTED, true);

        int toggleY = BODY_TOP + 58;
        int toggleX = leftX + 12;
        int toggleW = Math.min(104, Math.max(72, (leftW - 30) / 2));
        renderToggle(g, mouseX, mouseY, toggleX, toggleY, toggleW, "Source", !listSetsReplacement, SOURCE_ACCENT);
        renderToggle(g, mouseX, mouseY, toggleX + toggleW + 6, toggleY, toggleW, "Appearance", listSetsReplacement, APPEARANCE_ACCENT);

        renderEditorList(g, mouseX, mouseY);

        g.text(this.font, "Source", rightX + 12, BODY_TOP + 18, SOURCE_ACCENT, true);
        g.text(this.font, "Appearance", rightX + 12, BODY_TOP + 66, APPEARANCE_ACCENT, true);

        int previewY = BODY_TOP + 116;
        int previewH = 78;
        g.fill(rightX + 12, previewY, rightX + rightW - 12, previewY + previewH, 0x66110E14);
        outline(g, rightX + 12, previewY, rightW - 24, previewH, PANEL_BORDER);
        g.text(this.font, "Preview", rightX + 22, previewY + 8, MUTED, true);

        String from = source == null ? editorSourceText : source.getValue();
        String to = replacement == null ? editorReplacementText : replacement.getValue();
        ItemStack fromIcon = blockIcon(from), toIcon = blockIcon(to);
        int centerY = previewY + 40;
        int sourceX = rightX + rightW / 2 - 58;
        int targetX = rightX + rightW / 2 + 42;
        if (!fromIcon.isEmpty()) g.item(fromIcon, sourceX, centerY - 8);
        g.text(this.font, "→", rightX + rightW / 2 - 3, centerY - 4, MUTED, true);
        if (!toIcon.isEmpty()) g.item(toIcon, targetX, centerY - 8);

        int buttonY = previewY + previewH + 12;
        renderButton(g, mouseX, mouseY, rightX + 12, buttonY, rightW - 24, 22, "Apply", SOURCE_ACCENT, SOURCE_ACCENT, DARK_TEXT);
        int half = (rightW - 30) / 2;
        renderButton(g, mouseX, mouseY, rightX + 12, buttonY + 30, half, 22, "Reset block", PANEL, PANEL_BORDER, TEXT);
        String resetAllText = System.currentTimeMillis() < resetAllArmedUntil ? "Confirm reset all" : "Reset all";
        renderButton(g, mouseX, mouseY, rightX + 18 + half, buttonY + 30, half, 22, resetAllText, PANEL, DANGER, TEXT);

        if (!status.isBlank()) g.text(this.font, status, rightX + 12, Math.min(bodyBottom() - 16, buttonY + 62), MUTED, true);
    }

    private void renderToggle(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int y, int w, String label, boolean active, int accent) {
        g.fill(x, y, x + w, y + 20, active ? 0x22FFFFFF : PANEL);
        outline(g, x, y, w, 20, active ? accent : PANEL_BORDER);
        centeredShadow(g, label, x + w / 2, y + 6, active ? TEXT : MUTED);
    }

    private void renderEditorList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = OUTER + 12;
        int y = editorListY();
        int w = editorLeftWidth() - 24;
        int h = editorListHeight();
        List<String> ids = filteredBlocks(search == null ? searchText : search.getValue());
        editorScroll = clampScroll(editorScroll, ids.size(), h);
        String selectedId = BlockAppearanceManager.normalize(listSetsReplacement
                ? (replacement == null ? editorReplacementText : replacement.getValue())
                : (source == null ? editorSourceText : source.getValue()));

        g.enableScissor(x, y, x + w, y + h);
        int first = firstVisibleRow(editorScroll), yy = y + rowYOffset(editorScroll);
        for (int i = first; i < ids.size() && yy < y + h; i++, yy += ROW_H) {
            String id = ids.get(i);
            boolean hover = inside(mouseX, mouseY, x, yy, w - 5, ROW_H);
            boolean selected = id.equals(selectedId);
            if (selected) {
                g.fill(x, yy, x + w - 5, yy + ROW_H - 1, SELECTED);
                g.fill(x, yy, x + 3, yy + ROW_H - 1, listSetsReplacement ? APPEARANCE_ACCENT : SOURCE_ACCENT);
            } else if (hover) {
                g.fill(x, yy, x + w - 5, yy + ROW_H - 1, HOVER);
            }
            ItemStack icon = blockIcon(id);
            if (!icon.isEmpty()) g.item(icon, x + 5, yy + 2);
            String name = BlockCatalog.displayName(id);
            g.text(this.font, name, x + 26, yy + 6, TEXT, true);
            int idW = this.font.width(id);
            int right = x + w - 12;
            if (x + 32 + this.font.width(name) + 10 < right - idW) {
                g.text(this.font, id, right - idW, yy + 6, MUTED, true);
            }
        }
        g.disableScissor();
        renderScrollBar(g, x + w - 2, y, h, ids.size(), editorScroll);
    }

    private void renderGenericPanel(GuiGraphicsExtractor g) {
        panel(g, OUTER, BODY_TOP, usableWidth(), bodyHeight());
    }

    private void renderChangedList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<Map.Entry<String, BlockAppearanceConfig.Rule>> rules = sortedRules();
        int x = OUTER + 12, y = genericListY(), w = usableWidth() - 24, h = genericListHeight();
        changedScroll = clampScroll(changedScroll, rules.size(), h);
        g.enableScissor(x, y, x + w, y + h);
        int first = firstVisibleRow(changedScroll), yy = y + rowYOffset(changedScroll);
        for (int i = first; i < rules.size() && yy < y + h; i++, yy += ROW_H) {
            var entry = rules.get(i);
            String from = entry.getKey(), to = entry.getValue().replacement();
            boolean hover = inside(mouseX, mouseY, x, yy, w - 4, ROW_H);
            if (hover) g.fill(x, yy, x + w - 4, yy + ROW_H - 1, HOVER);
            ItemStack fromIcon = blockIcon(from), toIcon = blockIcon(to);
            if (!fromIcon.isEmpty()) g.item(fromIcon, x + 4, yy + 2);
            int tx = x + 26;
            g.text(this.font, from, tx, yy + 6, TEXT, true);
            int arrowX = Math.min(x + w / 2 - 16, tx + this.font.width(from) + 12);
            g.text(this.font, "→", arrowX, yy + 6, MUTED, true);
            int targetX = arrowX + 20;
            if (!toIcon.isEmpty()) g.item(toIcon, targetX, yy + 2);
            g.text(this.font, to, targetX + 22, yy + 6, TEXT, true);
            int removeW = 58, removeX = x + w - removeW - 6;
            renderButton(g, mouseX, mouseY, removeX, yy + 1, removeW, 18, "Remove", PANEL, DANGER, TEXT);
        }
        g.disableScissor();
        renderScrollBar(g, x + w - 2, y, h, rules.size(), changedScroll);
    }

    private void renderKuudraList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<String> blocks = availableKuudraBlocks();
        int x = OUTER + 12, y = genericListY(), w = usableWidth() - 24, h = genericListHeight();
        kuudraScroll = clampScroll(kuudraScroll, blocks.size(), h);
        if (blocks.isEmpty()) {
            g.text(this.font, "No unmodified Kuudra blocks recorded yet", x, y + 8, MUTED, true);
            return;
        }
        g.enableScissor(x, y, x + w, y + h);
        int first = firstVisibleRow(kuudraScroll), yy = y + rowYOffset(kuudraScroll);
        for (int i = first; i < blocks.size() && yy < y + h; i++, yy += ROW_H) {
            String id = blocks.get(i);
            boolean hover = inside(mouseX, mouseY, x, yy, w - 4, ROW_H);
            if (hover) g.fill(x, yy, x + w - 4, yy + ROW_H - 1, HOVER);
            ItemStack icon = blockIcon(id);
            if (!icon.isEmpty()) g.item(icon, x + 4, yy + 2);
            g.text(this.font, BlockCatalog.displayName(id), x + 26, yy + 6, TEXT, true);
            int idW = this.font.width(id);
            g.text(this.font, id, x + w - idW - 12, yy + 6, MUTED, true);
        }
        g.disableScissor();
        renderScrollBar(g, x + w - 2, y, h, blocks.size(), kuudraScroll);
    }

    private void renderPresets(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = OUTER + 12, y = BODY_TOP + 42, w = usableWidth() - 24;
        List<PresetManager.Preset> presets = PresetManager.builtIns();
        int yy = y;
        for (PresetManager.Preset preset : presets) {
            int cardH = 48;
            g.fill(x, yy, x + w, yy + cardH, 0x44110E14);
            outline(g, x, yy, w, cardH, PANEL_BORDER);
            g.text(this.font, preset.name(), x + 10, yy + 8, TEXT, true);
            g.text(this.font, preset.description(), x + 10, yy + 25, MUTED, true);
            int bx = x + w - 66;
            renderButton(g, mouseX, mouseY, bx, yy + 13, 54, 22, "Load", SOURCE_ACCENT, SOURCE_ACCENT, DARK_TEXT);
            yy += 56;
        }
    }

    private void renderSaves(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = OUTER + 12, w = usableWidth() - 24;
        int saveButtonX = x + Math.min(272, Math.max(112, w - 246));
        renderButton(g, mouseX, mouseY, saveButtonX, BODY_TOP + 33, 108, 22, "Save current", SOURCE_ACCENT, SOURCE_ACCENT, DARK_TEXT);
        renderButton(g, mouseX, mouseY, saveButtonX + 116, BODY_TOP + 33, 104, 22, "Import JSONs", PANEL, APPEARANCE_ACCENT, TEXT);

        List<String> names = SaveManager.names();
        int y = savesListY(), h = savesListHeight();
        saveScroll = clampScroll(saveScroll, names.size(), h);
        if (names.isEmpty()) {
            g.text(this.font, "No user saves yet", x, y + 8, MUTED, true);
            return;
        }
        g.enableScissor(x, y, x + w, y + h);
        int first = firstVisibleRow(saveScroll), yy = y + rowYOffset(saveScroll);
        for (int i = first; i < names.size() && yy < y + h; i++, yy += ROW_H) {
            String name = names.get(i);
            if (inside(mouseX, mouseY, x, yy, w - 4, ROW_H)) g.fill(x, yy, x + w - 4, yy + ROW_H - 1, HOVER);
            g.text(this.font, name, x + 6, yy + 6, TEXT, true);
            int deleteX = x + w - 54, renameX = deleteX - 62, exportX = renameX - 60, loadX = exportX - 50;
            renderMiniButton(g, mouseX, mouseY, loadX, yy + 1, 44, "Load", SOURCE_ACCENT);
            renderMiniButton(g, mouseX, mouseY, exportX, yy + 1, 54, "Export", APPEARANCE_ACCENT);
            renderMiniButton(g, mouseX, mouseY, renameX, yy + 1, 56, "Rename", PANEL_BORDER);
            renderMiniButton(g, mouseX, mouseY, deleteX, yy + 1, 48, "Delete", DANGER);
        }
        g.disableScissor();
        renderScrollBar(g, x + w - 2, y, h, names.size(), saveScroll);
    }

    private void renderMiniButton(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int y, int w, String text, int border) {
        g.fill(x, y, x + w, y + 18, PANEL);
        outline(g, x, y, w, 18, border);
        centeredShadow(g, text, x + w / 2, y + 5, TEXT);
    }

    private void renderConfig(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = OUTER + 12;
        int y = BODY_TOP + 42;
        int w = Math.min(300, usableWidth() - 24);
        KeyMapping key = KuudraAppearanceClient.openMenuKey();
        String label = listeningForKey ? "Press a key..." : "Open menu: " + key.getTranslatedKeyMessage().getString();
        renderButton(g, mouseX, mouseY, x, y, w, 24, label, PANEL, listeningForKey ? SOURCE_ACCENT : PANEL_BORDER, TEXT);
        renderButton(g, mouseX, mouseY, x, y + 34, w, 24, "Reset key to default", PANEL, PANEL_BORDER, TEXT);
        g.text(this.font, "/kba also opens this menu", x, y + 70, MUTED, true);
        if (listeningForKey) g.text(this.font, "Press any keyboard key. Esc cancels.", x, y + 88, SOURCE_ACCENT, true);
    }

    private void renderToast(GuiGraphicsExtractor g) {
        if (System.currentTimeMillis() >= toastUntil || toastMessage.isBlank()) return;
        int w = this.font.width(toastMessage) + 20;
        int x = this.width / 2 - w / 2;
        int y = BODY_TOP + 8;
        g.fill(x, y, x + w, y + 22, 0xEE17301F);
        outline(g, x, y, w, 22, SUCCESS);
        centeredShadow(g, toastMessage, this.width / 2, y + 7, SUCCESS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double amount = -scrollY * ROW_H * 2.0;
        if (tab == Tab.EDITOR) {
            int x = OUTER + 12, y = editorListY(), w = editorLeftWidth() - 24, h = editorListHeight();
            if (inside(mouseX, mouseY, x, y, w, h)) {
                editorScroll = clampScroll(editorScroll + amount, filteredBlocks(search == null ? searchText : search.getValue()).size(), h);
                return true;
            }
        } else if (tab == Tab.CHANGED) {
            changedScroll = clampScroll(changedScroll + amount, sortedRules().size(), genericListHeight());
            return true;
        } else if (tab == Tab.KUUDRA_BLOCKS) {
            kuudraScroll = clampScroll(kuudraScroll + amount, availableKuudraBlocks().size(), genericListHeight());
            return true;
        } else if (tab == Tab.SAVES) {
            saveScroll = clampScroll(saveScroll + amount, SaveManager.names().size(), savesListHeight());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x(), my = event.y();
            Tab clickedTab = tabAt(mx, my);
            if (clickedTab != null) { setTab(clickedTab); return true; }

            if (tab == Tab.EDITOR) {
                int leftX = OUTER, leftW = editorLeftWidth();
                int toggleY = BODY_TOP + 58;
                int toggleX = leftX + 12;
                int toggleW = Math.min(104, Math.max(72, (leftW - 30) / 2));
                if (inside(mx, my, toggleX, toggleY, toggleW, 20)) { listSetsReplacement = false; return true; }
                if (inside(mx, my, toggleX + toggleW + 6, toggleY, toggleW, 20)) { listSetsReplacement = true; return true; }

                int x = OUTER + 12, y = editorListY(), w = editorLeftWidth() - 24, h = editorListHeight();
                if (inside(mx, my, x, y, w, h)) {
                    List<String> ids = filteredBlocks(search == null ? searchText : search.getValue());
                    int index = (int) ((my - y + editorScroll) / ROW_H);
                    if (index >= 0 && index < ids.size()) {
                        String id = ids.get(index);
                        if (listSetsReplacement) {
                            editorReplacementText = id;
                            if (replacement != null) replacement.setValue(id);
                        } else {
                            editorSourceText = id;
                            if (source != null) source.setValue(id);
                        }
                        status = "Selected " + BlockCatalog.displayName(id) + (listSetsReplacement ? " as appearance" : " as source");
                        return true;
                    }
                }

                int rightX = editorRightX(), rightW = editorRightWidth();
                int previewY = BODY_TOP + 116, previewH = 78;
                int buttonY = previewY + previewH + 12;
                if (inside(mx, my, rightX + 12, buttonY, rightW - 24, 22)) { apply(); return true; }
                int half = (rightW - 30) / 2;
                if (inside(mx, my, rightX + 12, buttonY + 30, half, 22)) { reset(); return true; }
                if (inside(mx, my, rightX + 18 + half, buttonY + 30, half, 22)) { resetAll(); return true; }
            } else if (tab == Tab.CHANGED) {
                List<Map.Entry<String, BlockAppearanceConfig.Rule>> rules = sortedRules();
                int x = OUTER + 12, y = genericListY(), w = usableWidth() - 24, h = genericListHeight();
                if (inside(mx, my, x, y, w, h)) {
                    int index = (int) ((my - y + changedScroll) / ROW_H);
                    if (index >= 0 && index < rules.size()) {
                        var entry = rules.get(index);
                        int rowY = y + rowYOffset(changedScroll) + (index - firstVisibleRow(changedScroll)) * ROW_H;
                        int removeW = 58, removeX = x + w - removeW - 6;
                        if (inside(mx, my, removeX, rowY + 1, removeW, 18)) {
                            BlockAppearanceConfig.remove(entry.getKey());
                            status = "Removed " + BlockCatalog.displayName(entry.getKey());
                            refreshWorld();
                            init();
                            return true;
                        }
                        openEditorForRule(entry.getKey(), entry.getValue());
                        return true;
                    }
                }
            } else if (tab == Tab.KUUDRA_BLOCKS) {
                List<String> blocks = availableKuudraBlocks();
                int x = OUTER + 12, y = genericListY(), w = usableWidth() - 24, h = genericListHeight();
                if (inside(mx, my, x, y, w, h)) {
                    int index = (int) ((my - y + kuudraScroll) / ROW_H);
                    if (index >= 0 && index < blocks.size()) { openEditorForSource(blocks.get(index)); return true; }
                }
                int clearW = 128;
                if (inside(mx, my, this.width - OUTER - clearW - 12, BODY_TOP + 10, clearW, 20)) {
                    BlockAppearanceManager.clearObservedKuudraBlocks();
                    kuudraScroll = 0;
                    status = "Recorded Kuudra block list cleared";
                    init();
                    return true;
                }
            } else if (tab == Tab.PRESETS) {
                int x = OUTER + 12, y = BODY_TOP + 42, w = usableWidth() - 24, yy = y;
                for (PresetManager.Preset preset : PresetManager.builtIns()) {
                    int bx = x + w - 66;
                    if (inside(mx, my, bx, yy + 13, 54, 22)) { applyPreset(preset); return true; }
                    yy += 56;
                }
            } else if (tab == Tab.SAVES) {
                int x = OUTER + 12, w = usableWidth() - 24;
                int saveButtonX = x + Math.min(272, Math.max(112, w - 246));
                if (inside(mx, my, saveButtonX, BODY_TOP + 33, 108, 22)) { saveCurrent(); return true; }
                if (inside(mx, my, saveButtonX + 116, BODY_TOP + 33, 104, 22)) { importSaves(); return true; }

                List<String> names = SaveManager.names();
                int y = savesListY(), h = savesListHeight();
                if (inside(mx, my, x, y, w, h)) {
                    int index = (int) ((my - y + saveScroll) / ROW_H);
                    if (index >= 0 && index < names.size()) {
                        String name = names.get(index);
                        int rowY = y + rowYOffset(saveScroll) + (index - firstVisibleRow(saveScroll)) * ROW_H;
                        int deleteX = x + w - 54, renameX = deleteX - 62, exportX = renameX - 60, loadX = exportX - 50;
                        if (inside(mx, my, loadX, rowY + 1, 44, 18)) { loadSave(name); return true; }
                        if (inside(mx, my, exportX, rowY + 1, 54, 18)) { exportSave(name); return true; }
                        if (inside(mx, my, renameX, rowY + 1, 56, 18)) { renameSave(name); return true; }
                        if (inside(mx, my, deleteX, rowY + 1, 48, 18)) { deleteSave(name); return true; }
                    }
                }
            } else if (tab == Tab.CONFIG) {
                int x = OUTER + 12, y = BODY_TOP + 42, w = Math.min(300, usableWidth() - 24);
                if (inside(mx, my, x, y, w, 24)) {
                    listeningForKey = true;
                    return true;
                }
                if (inside(mx, my, x, y + 34, w, 24)) {
                    KeyMapping key = KuudraAppearanceClient.openMenuKey();
                    key.setKey(key.getDefaultKey());
                    KeyMapping.resetMapping();
                    minecraft.options.save();
                    listeningForKey = false;
                    status = "Menu key reset to default";
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (tab == Tab.CONFIG && listeningForKey) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                listeningForKey = false;
                return true;
            }
            KeyMapping key = KuudraAppearanceClient.openMenuKey();
            key.setKey(InputConstants.getKey(event));
            KeyMapping.resetMapping();
            minecraft.options.save();
            listeningForKey = false;
            status = "Menu key updated";
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Background and panel chrome first, then vanilla text fields/widgets, then KBA foreground.
        graphics.fill(0, 0, this.width, this.height, BG);
        renderHeaderAndTabs(graphics, mouseX, mouseY);
        if (tab == Tab.EDITOR) renderEditorBackground(graphics);
        else renderGenericPanel(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (tab == Tab.EDITOR) {
            renderEditorForeground(graphics, mouseX, mouseY);
        } else if (tab == Tab.CHANGED) {
            graphics.text(this.font, "Changed mappings (" + BlockAppearanceConfig.rules().size() + ")", OUTER + 12, BODY_TOP + 14, TEXT, true);
            graphics.text(this.font, "Click a mapping to edit it.", OUTER + 160, BODY_TOP + 14, MUTED, true);
            renderChangedList(graphics, mouseX, mouseY);
        } else if (tab == Tab.KUUDRA_BLOCKS) {
            String detail = KuudraDetector.isInKuudra()
                    ? "Blocks learned from the current Kuudra arena. Changed blocks are hidden."
                    : "Enter Kuudra once to populate this list.";
            graphics.text(this.font, detail, OUTER + 12, BODY_TOP + 14, MUTED, true);
            int clearW = 128;
            renderButton(graphics, mouseX, mouseY, this.width - OUTER - clearW - 12, BODY_TOP + 10, clearW, 20, "Clear recorded list", PANEL, PANEL_BORDER, TEXT);
            renderKuudraList(graphics, mouseX, mouseY);
        } else if (tab == Tab.PRESETS) {
            graphics.text(this.font, "Built-in presets", OUTER + 12, BODY_TOP + 14, TEXT, true);
            graphics.text(this.font, "Loading a preset copies it into your editable mappings.", OUTER + 116, BODY_TOP + 14, MUTED, true);
            renderPresets(graphics, mouseX, mouseY);
        } else if (tab == Tab.SAVES) {
            graphics.text(this.font, "User saves", OUTER + 12, BODY_TOP + 14, TEXT, true);
            renderSaves(graphics, mouseX, mouseY);
        } else if (tab == Tab.CONFIG) {
            graphics.text(this.font, "Config", OUTER + 12, BODY_TOP + 14, TEXT, true);
            renderConfig(graphics, mouseX, mouseY);
        }

        renderToast(graphics);
        renderFooter(graphics);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
