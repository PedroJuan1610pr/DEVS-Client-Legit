package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Tiempo de sesión, muertes y hits dados (solo info). */
public final class SessionStats extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting showTime = add(new BooleanSetting("Play Time", true));
    public final BooleanSetting showDeaths = add(new BooleanSetting("Deaths", true));
    public final BooleanSetting showHits = add(new BooleanSetting("Hits Landed", true));
    public final BooleanSetting resetBtn = add(new BooleanSetting("Reset Stats", false));

    private long startMs = System.currentTimeMillis();
    private int deaths, hits;
    private boolean wasDead;

    public SessionStats() { super("Session Stats", "Session playtime, deaths and hits landed.", "Misc"); }

    public void onHitLanded() { if (enabled()) hits++; }

    @Override public void tick() {
        if (resetBtn.get()) {
            resetBtn.set(false);
            startMs = System.currentTimeMillis();
            deaths = 0; hits = 0;
            Notifications.push("Session", "Stats reset");
        }
        if (mc.player == null) { wasDead = false; return; }
        boolean dead = mc.player.isDead() || mc.player.getHealth() <= 0;
        if (dead && !wasDead) deaths++;
        wasDead = dead;
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        var tr = mc.textRenderer;
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (showTime.get()) {
            long s = (System.currentTimeMillis() - startMs) / 1000;
            lines.add(String.format("Session: %d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60));
        }
        if (showDeaths.get()) lines.add("Deaths: " + deaths);
        if (showHits.get()) lines.add("Hits: " + hits);
        if (lines.isEmpty()) return;
        int w = 0, lh = 10;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        HudLayoutManager.begin(ctx, HudLayoutManager.SESSION_STATS, w + 6, lines.size() * lh + 4);
        int y = 2;
        for (String l : lines) { ctx.drawTextWithShadow(tr, l, 3, y, 0xFFCCCCCC); y += lh; }
        HudLayoutManager.end(ctx);
    }
}
