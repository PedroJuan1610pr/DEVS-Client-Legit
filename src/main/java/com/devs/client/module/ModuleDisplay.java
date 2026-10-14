package com.devs.client.module;

import com.devs.client.DevsClient;
import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.BooleanSetting;
import com.devs.client.setting.ColorSetting;
import com.devs.client.setting.ModeSetting;
import com.devs.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ArrayList de modulos activos — estilo limpio con barra de acento,
 * gradiente de color y sort por ancho real del texto.
 */
public final class ModuleDisplay extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting sort = add(new ModeSetting("Sort", "Length", "Length", "Alphabetical", "Category"));
    public final ModeSetting align = add(new ModeSetting("Align", "Right", "Right", "Left"));
    public final ModeSetting colorMode = add(new ModeSetting("Color Mode", "Gradient", "Static", "Gradient", "Rainbow"));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFFFF));
    public final BooleanSetting useAccent = add(new BooleanSetting("Use Accent Color", true));
    public final ColorSetting accent = add(new ColorSetting("Accent", 0xFFFF3B3B));
    public final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final BooleanSetting sideBar = add(new BooleanSetting("Side Bar", true));
    public final BooleanSetting hideSelf = add(new BooleanSetting("Hide Self", true));
    public final BooleanSetting showCategory = add(new BooleanSetting("Show Category", false));
    public final NumberSetting maxEntries = add(new NumberSetting("Max Entries", 32, 4, 64, 1));
    public final NumberSetting lineHeight = add(new NumberSetting("Line Height", 11, 9, 16, 1));
    public final NumberSetting pad = add(new NumberSetting("Padding", 4, 2, 10, 1));

    public ModuleDisplay() {
        super("Module Display", "Shows all active modules on screen (ArrayList).", "Misc");
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;

        List<Module> active = new ArrayList<>();
        for (Module m : DevsClient.MODULES.all()) {
            if (!m.enabled()) continue;
            if (hideSelf.get() && m == this) continue;
            active.add(m);
        }
        if (active.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;

        // Labels primero (para sort por ancho real en modo Length)
        record Row(Module mod, String label, int width) {}
        List<Row> rows = new ArrayList<>(active.size());
        for (Module m : active) {
            String lab = displayName(m);
            rows.add(new Row(m, lab, tr.getWidth(lab)));
        }

        switch (sort.get()) {
            case "Alphabetical" -> rows.sort(Comparator
                .comparing((Row r) -> r.label.toLowerCase()));
            case "Category" -> rows.sort(Comparator
                .comparing((Row r) -> r.mod.category())
                .thenComparing(r -> r.label.toLowerCase()));
            case "Length" -> rows.sort(Comparator
                // ancho en px (no chars): mas largo primero → cascada al alinear a la derecha
                .comparingInt((Row r) -> -r.width)
                .thenComparing(r -> r.label.toLowerCase()));
            default -> rows.sort(Comparator.comparingInt((Row r) -> -r.width));
        }

        int max = Math.min(rows.size(), maxEntries.get().intValue());
        if (rows.size() > max) rows = new ArrayList<>(rows.subList(0, max));

        int lh = lineHeight.get().intValue();
        int padding = pad.get().intValue();
        int maxW = 0;
        for (Row r : rows) maxW = Math.max(maxW, r.width);

        int barW = sideBar.get() ? 2 : 0;
        int contentW = maxW + padding * 2 + barW + 2;
        int contentH = rows.size() * lh + padding;

        HudLayoutManager.begin(ctx, HudLayoutManager.MODULE_DISPLAY, contentW, contentH);

        boolean right = "Right".equals(align.get());
        int baseAccent = (useAccent.get() ? accent.get() : color.get()) | 0xFF000000;
        int baseStatic = color.get() | 0xFF000000;
        String mode = colorMode.get();
        long time = System.currentTimeMillis();

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = padding / 2 + i * lh;
            int tw = r.width;
            int textX = right ? contentW - padding - tw : padding + barW;
            int rowLeft = right ? textX - 2 : 0;
            int rowRight = right ? contentW : textX + tw + 2;

            // fondo por fila (mas limpio que un bloque unico)
            if (background.get()) {
                int bgA = 0x88;
                // un poco mas opaco en la fila mas ancha (arriba si length)
                ctx.fill(rowLeft, y - 1, rowRight, y + lh - 2, (bgA << 24));
            }

            int col = switch (mode) {
                case "Rainbow" -> rainbow(time, i * 12);
                case "Gradient" -> lerpColor(baseAccent, baseStatic, rows.size() <= 1 ? 0f : (float) i / (rows.size() - 1));
                default -> baseAccent;
            };

            // barra lateral de acento
            if (sideBar.get()) {
                int bx = right ? contentW - barW : 0;
                ctx.fill(bx, y - 1, bx + barW, y + lh - 2, col);
            }

            if (shadow.get()) ctx.drawTextWithShadow(tr, r.label, textX, y, col);
            else ctx.drawText(tr, r.label, textX, y, col, false);
        }

        HudLayoutManager.end(ctx);
    }

    private String displayName(Module m) {
        return showCategory.get() ? m.category() + " · " + m.name() : m.name();
    }

    private static int rainbow(long time, int offset) {
        float hue = ((time / 20f) + offset) % 360f;
        return hsb(hue / 360f, 0.75f, 1f) | 0xFF000000;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int hsb(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
