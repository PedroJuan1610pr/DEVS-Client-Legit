package com.devs.client.module;

import com.devs.client.DevsClient;
import com.devs.client.gui.BindUtil;
import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Lista en HUD de módulos con keybind asignado. */
public final class KeybindList extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting onlyEnabled = add(new BooleanSetting("Only Enabled", false));
    public final NumberSetting maxRows = add(new NumberSetting("Max Rows", 12, 3, 30, 1));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFCCCCCC));

    public KeybindList() { super("Keybind List", "HUD list of modules that have a keybind.", "Misc"); }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        List<String> lines = new ArrayList<>();
        for (Module m : DevsClient.MODULES.all()) {
            if (m.keybind() < 0) continue;
            if (onlyEnabled.get() && !m.enabled()) continue;
            lines.add(BindUtil.name(m.keybind()) + "  " + m.name());
            if (lines.size() >= maxRows.get().intValue()) break;
        }
        if (lines.isEmpty()) return;
        var tr = mc.textRenderer;
        int w = 0, lh = 10;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        HudLayoutManager.begin(ctx, HudLayoutManager.KEYBIND_LIST, w + 6, lines.size() * lh + 4);
        int y = 2;
        for (String l : lines) { ctx.drawTextWithShadow(tr, l, 3, y, color.get() | 0xFF000000); y += lh; }
        HudLayoutManager.end(ctx);
    }
}
