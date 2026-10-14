package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.BooleanSetting;
import com.devs.client.setting.ColorSetting;
import com.devs.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Texto custom en el HUD, multi-linea, color y posicion (HUD Editor / H).
 * En el campo Text usa \n para saltos de linea, o edita Line 1..4.
 */
public final class CustomDisplay extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final StringSetting text = add(new StringSetting("Text", "Hello DEVS", 512));
    public final StringSetting line2 = add(new StringSetting("Line 2", "", 128));
    public final StringSetting line3 = add(new StringSetting("Line 3", "", 128));
    public final StringSetting line4 = add(new StringSetting("Line 4", "", 128));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFFFF));
    public final BooleanSetting shadow = add(new BooleanSetting("Shadow", true));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final ColorSetting bgColor = add(new ColorSetting("Background Color", 0x90000000));
    public final BooleanSetting accentBar = add(new BooleanSetting("Accent Bar", true));
    public final ColorSetting accent = add(new ColorSetting("Accent Color", 0xFF4DD2FF));

    public CustomDisplay() {
        super("Custom Display", "Custom multi-line text on the HUD (positionable).", "Misc");
    }

    private List<String> lines() {
        List<String> out = new ArrayList<>();
        String t = text.get() == null ? "" : text.get();
        if (!t.isEmpty()) {
            for (String part : t.replace("\\n", "\n").split("\n", -1)) {
                out.add(part);
            }
        }
        for (StringSetting s : new StringSetting[]{line2, line3, line4}) {
            String v = s.get();
            if (v != null && !v.isEmpty()) out.add(v);
        }
        if (out.isEmpty()) out.add("");
        return out;
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        List<String> lines = lines();
        var tr = mc.textRenderer;
        int lh = 10, w = 0;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int contentW = Math.max(20, w + 8), contentH = Math.max(12, lines.size() * lh + 6);

        HudLayoutManager.begin(ctx, HudLayoutManager.CUSTOM_DISPLAY, contentW, contentH);
        int x = 4, y = 3;
        if (background.get()) {
            int bg = bgColor.get();
            if ((bg & 0xFF000000) == 0) bg |= 0x90000000;
            ctx.fill(0, 0, contentW, contentH, bg);
        }
        if (accentBar.get()) {
            int ac = accent.get() | 0xFF000000;
            ctx.fill(0, 0, 2, contentH, ac);
        }
        int col = color.get() | 0xFF000000;
        for (String l : lines) {
            if (shadow.get()) ctx.drawTextWithShadow(tr, l, x, y, col);
            else ctx.drawText(tr, l, x, y, col, false);
            y += lh;
        }
        HudLayoutManager.end(ctx);
    }
}
