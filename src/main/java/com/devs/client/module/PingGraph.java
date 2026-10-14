package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayDeque;

/** Ping actual + mini gráfica de historial. */
public final class PingGraph extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting samples = add(new NumberSetting("Samples", 40, 10, 80, 5));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF4DD2FF));
    public final ColorSetting badColor = add(new ColorSetting("High Ping", 0xFFFF4D6D));

    private final ArrayDeque<Integer> hist = new ArrayDeque<>();
    private int tick;

    public PingGraph() { super("Ping Graph", "Ping value with a small history graph.", "Misc"); }

    @Override public void tick() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        tick++;
        if (tick % 10 != 0) return; // cada 0.5s
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        int ping = e != null ? e.getLatency() : 0;
        hist.addLast(ping);
        while (hist.size() > samples.get().intValue()) hist.removeFirst();
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        int ping = hist.isEmpty() ? 0 : hist.peekLast();
        String line = "Ping: " + ping + " ms";
        int graphW = Math.max(40, hist.size());
        int w = Math.max(mc.textRenderer.getWidth(line) + 6, graphW + 6);
        int h = 14 + 18;
        HudLayoutManager.begin(ctx, HudLayoutManager.PING_GRAPH, w, h);
        int col = ping > 150 ? badColor.get() : color.get();
        ctx.drawTextWithShadow(mc.textRenderer, line, 3, 2, col | 0xFF000000);
        // graph
        int baseY = 28, maxH = 14;
        int maxPing = 1;
        for (int p : hist) maxPing = Math.max(maxPing, p);
        int i = 0;
        for (int p : hist) {
            int bh = Math.max(1, p * maxH / Math.max(50, maxPing));
            int c = p > 150 ? badColor.get() : color.get();
            ctx.fill(3 + i, baseY - bh, 4 + i, baseY, (0xC0 << 24) | (c & 0xFFFFFF));
            i++;
        }
        HudLayoutManager.end(ctx);
    }
}
