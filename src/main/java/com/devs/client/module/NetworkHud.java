package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

/** Ping + TPS estimado en HUD. */
public final class NetworkHud extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting showPing = add(new BooleanSetting("Ping", true));
    public final BooleanSetting showTps = add(new BooleanSetting("TPS", true));
    public final BooleanSetting showFps = add(new BooleanSetting("FPS", false));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));

    private final double[] tickDeltas = new double[20];
    private int ti;
    private long lastNano = System.nanoTime();

    public NetworkHud() {
        super("Network HUD", "Ping, estimated TPS and optional FPS.", "Misc");
    }

    @Override public void tick() {
        long now = System.nanoTime();
        double dt = (now - lastNano) / 1_000_000_000.0;
        lastNano = now;
        tickDeltas[ti++ % tickDeltas.length] = dt;
    }

    private double tps() {
        double sum = 0;
        int n = 0;
        for (double d : tickDeltas) {
            if (d <= 0) continue;
            sum += d; n++;
        }
        if (n == 0) return 20;
        double avg = sum / n;
        return Math.min(20.0, 1.0 / Math.max(0.001, avg));
    }

    private int ping() {
        if (mc.getNetworkHandler() == null || mc.player == null) return -1;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? -1 : e.getLatency();
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        var tr = mc.textRenderer;
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (showPing.get()) {
            int p = ping();
            lines.add(p < 0 ? "Ping: ?" : "Ping: " + p + "ms");
        }
        if (showTps.get()) lines.add(String.format("TPS: %.1f", tps()));
        if (showFps.get()) lines.add("FPS: " + mc.getCurrentFps());
        if (lines.isEmpty()) return;
        int w = 60;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int h = lines.size() * 10 + 4;
        HudLayoutManager.begin(ctx, HudLayoutManager.NETWORK_HUD, w + 8, h);
        if (background.get()) ctx.fill(0, 0, w + 8, h, 0x90101018);
        int y = 2;
        for (String l : lines) {
            int col = 0xFFFFFFFF;
            if (l.startsWith("Ping")) {
                int p = ping();
                col = p < 0 ? 0xFFAAAAAA : p < 80 ? 0xFF6FE3A1 : p < 150 ? 0xFFFFD94D : 0xFFFF4D6D;
            } else if (l.startsWith("TPS")) {
                double t = tps();
                col = t > 18 ? 0xFF6FE3A1 : t > 15 ? 0xFFFFD94D : 0xFFFF4D6D;
            }
            ctx.drawTextWithShadow(tr, l, 4, y, col);
            y += 10;
        }
        HudLayoutManager.end(ctx);
    }
}
