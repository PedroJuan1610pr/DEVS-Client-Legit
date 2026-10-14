package com.devs.client.module;

import com.devs.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import com.devs.client.hud.HudLayoutManager;

/** Reconecta tras desconexion (Autism AutoReconnect). */
public final class AutoReconnect extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting delay = add(new NumberSetting("Delay (s)", 3.5, 0.5, 30, 0.5));
    private ServerInfo lastServer;
    private long reconnectAt;
    private boolean pending;

    public AutoReconnect() { super("AutoReconnect", "Reconnects automatically after disconnect.", "Misc"); }

    public void remember(ServerInfo info) {
        if (info != null) lastServer = info;
    }

    /** Captura el server actual cada tick mientras estas conectado. */
    private void capture() {
        try {
            ServerInfo cur = mc.getCurrentServerEntry();
            if (cur != null) lastServer = cur;
        } catch (Throwable ignored) {}
    }

    public void onDisconnect() {
        if (!enabled()) return;
        capture();
        if (lastServer == null) return;
        pending = true;
        reconnectAt = System.currentTimeMillis() + (long) (delay.get() * 1000);
    }

    @Override public void tick() {
        if (!enabled()) return;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            capture();
            pending = false;
            return;
        }
        if (!pending || lastServer == null) return;
        if (System.currentTimeMillis() < reconnectAt) return;
        pending = false;
        try {
            ServerAddress addr = ServerAddress.parse(lastServer.address);
            ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, addr, lastServer, false, null);
        } catch (Throwable t) {
            if (mc.player != null)
                mc.player.sendMessage(Text.literal("[DEVS] AutoReconnect failed")
                    .formatted(Formatting.RED));
        }
    }

    @Override public void renderHud(DrawContext ctx) {
        if (!pending || lastServer == null || mc.player != null) return;
        long left = Math.max(0, reconnectAt - System.currentTimeMillis());
        String line = String.format("Reconnecting to %s in %.1fs…", lastServer.name, left / 1000.0);
        int w = mc.textRenderer.getWidth(line) + 8;
        // no layout id registered — draw top-center raw
        int sw = mc.getWindow().getScaledWidth();
        ctx.drawTextWithShadow(mc.textRenderer, line, (sw - w) / 2, 12, 0xFFFFD94D);
    }
}
