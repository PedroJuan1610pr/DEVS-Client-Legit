package com.devs.client.module;

import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Waypoints estilo Autism: puntos nombrados + muertes, con labels/tracers. */
public final class Waypoints extends Module {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final BooleanSetting onDeath = add(new BooleanSetting("Waypoint On Death", true));
    public final BooleanSetting trackDeaths = add(new BooleanSetting("Track Deaths When Off", false));
    public final NumberSetting deathMemory = add(new NumberSetting("Death Memory", 10, 1, 50, 1));
    public final ColorSetting deathColor = add(new ColorSetting("Death Color", 0xFFFF3B3B));
    public final ColorSetting defaultColor = add(new ColorSetting("Default Color", 0xFF3BD7FF));
    public final BooleanSetting showDistance = add(new BooleanSetting("Show Distance", true));
    public final BooleanSetting tracers = add(new BooleanSetting("Tracers", true));
    public final BooleanSetting boxes = add(new BooleanSetting("Boxes", true));
    public final BooleanSetting addHere = add(new BooleanSetting("Add Here", false));
    public final BooleanSetting clearAll = add(new BooleanSetting("Clear All", false));

    private record Wp(String name, double x, double y, double z, String dim, int color, boolean death) {}
    private final List<Wp> points = new ArrayList<>();
    private boolean wasDead;
    private boolean loaded;

    public Waypoints() { super("Waypoints", "Save and render named destinations (Autism-inspired).", "Render"); }

    private Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("waypoints.json");
    }

    private void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            Path f = file();
            if (!Files.isRegularFile(f)) return;
            JsonArray arr = JsonParser.parseString(Files.readString(f)).getAsJsonArray();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                points.add(new Wp(
                    o.get("name").getAsString(),
                    o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble(),
                    o.get("dim").getAsString(),
                    o.has("color") ? o.get("color").getAsInt() : 0xFF3BD7FF,
                    o.has("death") && o.get("death").getAsBoolean()
                ));
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        try {
            JsonArray arr = new JsonArray();
            for (Wp w : points) {
                JsonObject o = new JsonObject();
                o.addProperty("name", w.name());
                o.addProperty("x", w.x()); o.addProperty("y", w.y()); o.addProperty("z", w.z());
                o.addProperty("dim", w.dim());
                o.addProperty("color", w.color());
                o.addProperty("death", w.death());
                arr.add(o);
            }
            Path dir = file().getParent();
            Files.createDirectories(dir);
            Files.writeString(file(), GSON.toJson(arr));
        } catch (Exception ignored) {}
    }

    @Override public void tick() {
        ensureLoaded();
        if (mc.player == null || mc.world == null) { wasDead = false; return; }

        if (addHere.get()) {
            addHere.set(false);
            Identifier dim = mc.world.getRegistryKey().getValue();
            double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
            String name = String.format("WP %d", points.size() + 1);
            points.add(new Wp(name, x, y, z, dim.toString(), defaultColor.get(), false));
            save();
            mc.player.sendMessage(Text.literal(String.format("[DEVS] Waypoint '%s' @ %d %d %d",
                name, Math.round(x), Math.round(y), Math.round(z))).formatted(Formatting.AQUA));
        }
        if (clearAll.get()) {
            clearAll.set(false);
            points.clear();
            save();
            if (mc.player != null) mc.player.sendMessage(Text.literal("[DEVS] All waypoints cleared.").formatted(Formatting.YELLOW));
        }

        boolean dead = mc.player.isDead() || mc.player.getHealth() <= 0;
        if (dead && !wasDead) {
            boolean should = enabled() ? onDeath.get() : trackDeaths.get();
            if (should) {
                Identifier dim = mc.world.getRegistryKey().getValue();
                double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
                points.add(new Wp("Death", x, y, z, dim.toString(), deathColor.get(), true));
                // cap death memory
                int max = deathMemory.get().intValue();
                int deaths = 0;
                for (int i = points.size() - 1; i >= 0; i--) if (points.get(i).death()) deaths++;
                if (deaths > max) {
                    Iterator<Wp> it = points.iterator();
                    while (it.hasNext() && deaths > max) {
                        if (it.next().death()) { it.remove(); deaths--; }
                    }
                }
                save();
                if (enabled() && mc.player != null)
                    mc.player.sendMessage(Text.literal(String.format("[DEVS] Death waypoint @ %d %d %d",
                        Math.round(x), Math.round(y), Math.round(z))).formatted(Formatting.RED));
            }
        }
        wasDead = dead;
    }

    @Override public void render() {
        if (!enabled()) return;
        ensureLoaded();
        if (mc.player == null || mc.world == null || points.isEmpty()) return;
        String dim = mc.world.getRegistryKey().getValue().toString();
        var from = tracers.get() ? HighlightRenderer.tracerOrigin() : null;
        for (Wp w : points) {
            if (!dim.equals(w.dim())) continue;
            Vec3d pos = new Vec3d(w.x(), w.y(), w.z());
            double dist = mc.player.getEyePos().distanceTo(pos);
            int c = w.color() | 0xFF000000;
            if (boxes.get()) {
                HighlightRenderer.drawBox(Box.of(pos.add(0, 0.5, 0), 0.6, 1.0, 0.6),
                    HighlightMode.OUTLINE_FILLED, HighlightRenderer.withOpacity(c, 0.45f), 1.5f);
            }
            if (tracers.get()) HighlightRenderer.line(from, pos.add(0, 0.5, 0), c, 1.5f);
            String label = w.name();
            if (showDistance.get()) label += " [" + Math.round(dist) + "m]";
            HighlightRenderer.label(label, pos.add(0, 1.4, 0), c, HighlightRenderer.labelScale(dist));
        }
    }
}
