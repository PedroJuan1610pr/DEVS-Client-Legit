package com.devs.client.module;

import com.devs.client.McCompat;

import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

/** Bordes de chunk y opcionalmente líneas de sección (subchunk Y). */
public final class ChunkBorders extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting chunkRadius = add(new NumberSetting("Chunk Radius", 0, 0, 4, 1));
    public final BooleanSetting sections = add(new BooleanSetting("Section Lines", false));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF4DD2FF));
    public final ColorSetting dimColor = add(new ColorSetting("Neighbor Color", 0xFF2A6A88));

    public ChunkBorders() { super("Chunk Borders", "Draws chunk borders and optional section lines.", "World"); }

    @Override public void render() {
        if (mc.player == null || mc.world == null) return;
        ChunkPos cp = mc.player.getChunkPos();
        int r = chunkRadius.get().intValue();
        boolean prev = HighlightRenderer.throughWalls;
        HighlightRenderer.throughWalls = false;
        try {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean main = dx == 0 && dz == 0;
                    int col = main ? color.get() : dimColor.get();
                    float a = main ? 0.85f : 0.4f;
                    drawChunk(McCompat.chunkX(cp) + dx, McCompat.chunkZ(cp) + dz, col, a, main && sections.get());
                }
            }
        } finally {
            HighlightRenderer.throughWalls = prev;
        }
    }

    private void drawChunk(int cx, int cz, int col, float alpha, boolean sectionLines) {
        int x0 = cx << 4, z0 = cz << 4;
        int x1 = x0 + 16, z1 = z0 + 16;
        int yBot = mc.world.getBottomY();
        int yTop = mc.world.getBottomY() + mc.world.getHeight();
        int[][] corners = {{x0,z0},{x1,z0},{x1,z1},{x0,z1}};
        for (int[] c : corners) {
            HighlightRenderer.line(new Vec3d(c[0], yBot, c[1]), new Vec3d(c[0], yTop, c[1]),
                HighlightRenderer.withOpacity(col, alpha * 0.5f), 1.2f);
        }
        double y = mc.player.getY() + 0.05;
        HighlightRenderer.line(new Vec3d(x0, y, z0), new Vec3d(x1, y, z0), HighlightRenderer.withOpacity(col, alpha), 1.8f);
        HighlightRenderer.line(new Vec3d(x1, y, z0), new Vec3d(x1, y, z1), HighlightRenderer.withOpacity(col, alpha), 1.8f);
        HighlightRenderer.line(new Vec3d(x1, y, z1), new Vec3d(x0, y, z1), HighlightRenderer.withOpacity(col, alpha), 1.8f);
        HighlightRenderer.line(new Vec3d(x0, y, z1), new Vec3d(x0, y, z0), HighlightRenderer.withOpacity(col, alpha), 1.8f);

        if (sectionLines) {
            int py = mc.player.getBlockPos().getY();
            for (int sy = ((py - 32) >> 4) << 4; sy <= py + 32; sy += 16) {
                if (sy < yBot || sy > yTop) continue;
                double yy = sy + 0.02;
                HighlightRenderer.line(new Vec3d(x0, yy, z0), new Vec3d(x1, yy, z0), HighlightRenderer.withOpacity(col, 0.35f), 1f);
                HighlightRenderer.line(new Vec3d(x1, yy, z0), new Vec3d(x1, yy, z1), HighlightRenderer.withOpacity(col, 0.35f), 1f);
                HighlightRenderer.line(new Vec3d(x1, yy, z1), new Vec3d(x0, yy, z1), HighlightRenderer.withOpacity(col, 0.35f), 1f);
                HighlightRenderer.line(new Vec3d(x0, yy, z1), new Vec3d(x0, yy, z0), HighlightRenderer.withOpacity(col, 0.35f), 1f);
            }
        }
    }
}
