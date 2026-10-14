package com.devs.client.module;

import com.devs.client.McCompat;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;

import java.util.Random;

/**
 * Indicador de slime chunk.
 * En multiplayer el seed puede no ser conocido: modo "HUD only" usa heurística desactivada
 * y solo marca si activas seed manual (singleplayer / seed conocido).
 */
public final class SlimeChunks extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting hud = add(new BooleanSetting("HUD", true));
    public final BooleanSetting outline = add(new BooleanSetting("Chunk Outline", true));
    public final StringSetting seedStr = add(new StringSetting("World Seed", "0"));
    public final BooleanSetting useSeed = add(new BooleanSetting("Use Seed", false));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF6FE3A1));

    public SlimeChunks() { super("Slime Chunks", "Shows if current chunk is a slime chunk (needs seed).", "World"); }

    private boolean isSlime(int chunkX, int chunkZ, long seed) {
        Random rnd = new Random(seed + (long) chunkX * chunkX * 4987142L + (long) chunkX * 5947611L
            + (long) chunkZ * chunkZ * 4392871L + (long) chunkZ * 389711L ^ 987234911L);
        return rnd.nextInt(10) == 0;
    }

    private long seed() {
        try { return Long.parseLong(seedStr.get().trim()); }
        catch (Exception e) { return 0L; }
    }

    @Override public void renderHud(DrawContext ctx) {
        if (!hud.get() || !useSeed.get() || mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        ChunkPos cp = mc.player.getChunkPos();
        boolean slime = isSlime(McCompat.chunkX(cp), McCompat.chunkZ(cp), seed());
        String line = slime ? "Slime chunk: YES" : "Slime chunk: no";
        int w = mc.textRenderer.getWidth(line) + 6;
        HudLayoutManager.begin(ctx, HudLayoutManager.SLIME_CHUNKS, w, 14);
        ctx.drawTextWithShadow(mc.textRenderer, line, 3, 2, (slime ? color.get() : 0xFF888888) | 0xFF000000);
        HudLayoutManager.end(ctx);
    }

    @Override public void render() {
        if (!outline.get() || !useSeed.get() || mc.player == null || mc.world == null) return;
        ChunkPos cp = mc.player.getChunkPos();
        if (!isSlime(McCompat.chunkX(cp), McCompat.chunkZ(cp), seed())) return;
        int x0 = cp.getStartX(), z0 = cp.getStartZ();
        int y = mc.player.getBlockPos().getY();
        Box box = new Box(x0, y - 1, z0, x0 + 16, y + 1, z0 + 16);
        boolean prev = HighlightRenderer.throughWalls;
        HighlightRenderer.throughWalls = false;
        try {
            HighlightRenderer.drawBox(box, HighlightMode.OUTLINE, HighlightRenderer.withOpacity(color.get(), 0.5f), 1.5f);
        } finally {
            HighlightRenderer.throughWalls = prev;
        }
    }
}
