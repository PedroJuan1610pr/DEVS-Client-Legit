package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.LightType;

/** Nivel de luz del bloque bajo tus pies (HUD + outline opcional). */
public final class LightLevel extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting hud = add(new BooleanSetting("HUD", true));
    public final BooleanSetting outline = add(new BooleanSetting("Block Outline", false));
    public final NumberSetting unsafe = add(new NumberSetting("Unsafe Below", 8, 0, 15, 1));
    public final ColorSetting safeColor = add(new ColorSetting("Safe Color", 0xFF6FE3A1));
    public final ColorSetting unsafeColor = add(new ColorSetting("Unsafe Color", 0xFFFF4D6D));

    public LightLevel() { super("Light Level", "Shows block light under your feet (mob-spawn awareness).", "World"); }

    @Override public void renderHud(DrawContext ctx) {
        if (!hud.get() || mc.player == null || mc.world == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        BlockPos pos = mc.player.getBlockPos();
        int block = mc.world.getLightLevel(LightType.BLOCK, pos);
        int sky = mc.world.getLightLevel(LightType.SKY, pos);
        int combined = mc.world.getLightLevel(pos);
        boolean bad = block < unsafe.get().intValue();
        String line = String.format("Light: %d (block %d / sky %d)", combined, block, sky);
        int w = mc.textRenderer.getWidth(line) + 6;
        HudLayoutManager.begin(ctx, HudLayoutManager.LIGHT_LEVEL, w, 14);
        ctx.drawTextWithShadow(mc.textRenderer, line, 3, 2, (bad ? unsafeColor.get() : safeColor.get()) | 0xFF000000);
        HudLayoutManager.end(ctx);
    }

    @Override public void render() {
        if (!outline.get() || mc.player == null || mc.world == null) return;
        BlockPos pos = mc.player.getBlockPos().down();
        int block = mc.world.getLightLevel(LightType.BLOCK, mc.player.getBlockPos());
        int col = block < unsafe.get().intValue() ? unsafeColor.get() : safeColor.get();
        boolean prev = HighlightRenderer.throughWalls;
        HighlightRenderer.throughWalls = false;
        try {
            HighlightRenderer.drawBox(new Box(pos), HighlightMode.OUTLINE, HighlightRenderer.withOpacity(col, 0.7f), 1.5f);
        } finally {
            HighlightRenderer.throughWalls = prev;
        }
    }
}
