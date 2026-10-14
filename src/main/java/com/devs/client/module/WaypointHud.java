package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Flechas + distancia a waypoints en HUD. */
public final class WaypointHud extends Module {
    private record Wp(String name, double x, double y, double z, String dim) {}
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting max = add(new NumberSetting("Max Shown", 5, 1, 12, 1));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF3BD7FF));

    private final List<Wp> cache = new ArrayList<>();
    private int reloadAt;

    public WaypointHud() {
        super("Waypoint HUD", "On-screen arrows and distance to waypoints.", "Render");
    }

    private void reload() {
        cache.clear();
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("waypoints.json");
            if (!Files.isRegularFile(f)) return;
            JsonArray arr = JsonParser.parseString(Files.readString(f)).getAsJsonArray();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                cache.add(new Wp(
                    o.get("name").getAsString(),
                    o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble(),
                    o.get("dim").getAsString()
                ));
            }
        } catch (Exception ignored) {}
    }

    @Override public void tick() {
        if ((reloadAt++ % 40) == 0) reload();
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.world == null || cache.isEmpty()) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        String dim = mc.world.getRegistryKey().getValue().toString();
        var tr = mc.textRenderer;
        record Row(String text, double dist, float yaw) {}
        List<Row> rows = new ArrayList<>();
        for (Wp w : cache) {
            if (!w.dim().equals(dim) && !w.dim().contains(mc.world.getRegistryKey().getValue().getPath())) continue;
            double dx = w.x() - mc.player.getX();
            double dz = w.z() - mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            float bearing = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
            float rel = MathHelper.wrapDegrees(bearing - mc.player.getYaw());
            String arrow = arrowFor(rel);
            rows.add(new Row(arrow + " " + w.name() + " " + Math.round(dist) + "m", dist, rel));
        }
        rows.sort((a, b) -> Double.compare(a.dist(), b.dist()));
        int n = Math.min(rows.size(), max.get().intValue());
        if (n == 0) return;
        int maxW = 80;
        for (int i = 0; i < n; i++) maxW = Math.max(maxW, tr.getWidth(rows.get(i).text()));
        int h = n * 11 + 4;
        HudLayoutManager.begin(ctx, HudLayoutManager.WAYPOINT_HUD, maxW + 8, h);
        if (background.get()) ctx.fill(0, 0, maxW + 8, h, 0x90101018);
        int y = 2;
        int col = color.get() | 0xFF000000;
        for (int i = 0; i < n; i++) {
            ctx.drawTextWithShadow(tr, rows.get(i).text(), 4, y, col);
            y += 11;
        }
        HudLayoutManager.end(ctx);
    }

    private static String arrowFor(float relYaw) {
        // -180..180 → 8 directions
        float a = MathHelper.wrapDegrees(relYaw);
        if (a >= -22.5 && a < 22.5) return "↑";
        if (a >= 22.5 && a < 67.5) return "↗";
        if (a >= 67.5 && a < 112.5) return "→";
        if (a >= 112.5 && a < 157.5) return "↘";
        if (a >= 157.5 || a < -157.5) return "↓";
        if (a >= -157.5 && a < -112.5) return "↙";
        if (a >= -112.5 && a < -67.5) return "←";
        return "↖";
    }
}
