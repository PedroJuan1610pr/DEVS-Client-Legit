package com.devs.client.module;

import com.devs.client.McCompat;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class HudInfo extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting coords = add(new BooleanSetting("Coordinates", true));
    public final BooleanSetting otherDim = add(new BooleanSetting("Nether/Overworld Coords", true));
    public final BooleanSetting chunk = add(new BooleanSetting("Chunk Coords", true));
    public final BooleanSetting groundY = add(new BooleanSetting("Height Above Ground", true));
    public final BooleanSetting direction = add(new BooleanSetting("Direction", true));
    public final BooleanSetting biome = add(new BooleanSetting("Biome", true));
    public final BooleanSetting fps = add(new BooleanSetting("FPS", true));

    public HudInfo() { super("HUD Info", "Coordinates, chunk, ground height, direction, biome and FPS.", "Misc"); }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.world == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        if (ScreenshotClean.shouldHideHud()) return;
        var p = mc.player;
        List<String> lines = new ArrayList<>();

        if (coords.get()) lines.add(String.format("XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ()));
        if (otherDim.get()) {
            String dim = mc.world.getRegistryKey().getValue().getPath();
            if (dim.equals("overworld")) lines.add(String.format("Nether: %d / %d", Math.round(p.getX() / 8), Math.round(p.getZ() / 8)));
            else if (dim.equals("the_nether")) lines.add(String.format("Overworld: %d / %d", Math.round(p.getX() * 8), Math.round(p.getZ() * 8)));
        }
        if (chunk.get()) {
            lines.add(String.format("Chunk: %d %d (section %d)", McCompat.chunkX(p.getChunkPos()), McCompat.chunkZ(p.getChunkPos()), p.getBlockPos().getY() >> 4));
        }
        if (groundY.get()) {
            BlockPos feet = p.getBlockPos();
            int gy = feet.getY();
            for (int y = feet.getY(); y >= mc.world.getBottomY(); y--) {
                if (!mc.world.getBlockState(new BlockPos(feet.getX(), y, feet.getZ())).isAir()) {
                    gy = y + 1; break;
                }
            }
            lines.add(String.format("Above ground: %d", Math.max(0, feet.getY() - gy + 1)));
        }
        if (direction.get()) {
            Direction f = p.getHorizontalFacing();
            String axis = switch (f) {
                case NORTH -> "-Z"; case SOUTH -> "+Z"; case WEST -> "-X"; default -> "+X";
            };
            lines.add("Facing: " + pretty(f.asString()) + " (" + axis + ") · " + String.format("%.0f°", p.getYaw() % 360));
        }
        if (biome.get()) {
            String b = mc.world.getBiome(p.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("unknown");
            lines.add("Biome: " + pretty(b));
        }
        if (fps.get()) lines.add("FPS: " + mc.getCurrentFps());
        if (lines.isEmpty()) return;

        int lh = 10, w = 0;
        for (String l : lines) w = Math.max(w, mc.textRenderer.getWidth(l));
        HudLayoutManager.begin(ctx, HudLayoutManager.HUD_INFO, w + 6, lines.size() * lh + 4);
        int y = 2;
        for (String l : lines) {
            ctx.drawTextWithShadow(mc.textRenderer, l, 3, y, 0xFFE0E0E0);
            y += lh;
        }
        HudLayoutManager.end(ctx);
    }

    private static String pretty(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).replace('_', ' ');
    }
}
