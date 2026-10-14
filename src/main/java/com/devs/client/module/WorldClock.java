package com.devs.client.module;

import com.devs.client.McCompat;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Hora del mundo y fase día/noche. */
public final class WorldClock extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting digital = add(new BooleanSetting("24h Format", true));
    public final BooleanSetting phase = add(new BooleanSetting("Day Phase", true));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFD94D));

    public WorldClock() { super("World Clock", "Shows in-world time and day/night phase.", "Misc"); }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.world == null || mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        long t = McCompat.dayTime(mc.world) % 24000L;
        // 0 = 6:00, 6000 = noon, 12000 = 18:00, 18000 = midnight
        long total = (t + 6000) % 24000;
        int hour = (int) (total / 1000);
        int min = (int) ((total % 1000) * 60 / 1000);
        String time;
        if (digital.get()) time = String.format("%02d:%02d", hour, min);
        else {
            int h12 = hour % 12; if (h12 == 0) h12 = 12;
            time = String.format("%d:%02d %s", h12, min, hour < 12 ? "AM" : "PM");
        }
        String line = "Time: " + time;
        if (phase.get()) {
            String ph = t < 0 ? "?" : t < 6000 ? "Morning" : t < 12000 ? "Day" : t < 13000 ? "Sunset" : t < 23000 ? "Night" : "Sunrise";
            line += " · " + ph;
        }
        int w = mc.textRenderer.getWidth(line) + 6;
        HudLayoutManager.begin(ctx, HudLayoutManager.WORLD_CLOCK, w, 14);
        ctx.drawTextWithShadow(mc.textRenderer, line, 3, 2, color.get() | 0xFF000000);
        HudLayoutManager.end(ctx);
    }
}
