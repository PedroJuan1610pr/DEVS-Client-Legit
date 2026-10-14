package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/** Distancia al bloque o entidad bajo la mira. */
public final class ReachDisplay extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFD94D));
    public final BooleanSetting always = add(new BooleanSetting("Always Show", false));

    public ReachDisplay() { super("Reach Display", "Distance to the block or entity you look at.", "Misc"); }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.world == null || mc.getDebugHud().shouldShowDebugHud()) return;
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            if (!always.get()) return;
            String line = "Reach: —";
            int w = mc.textRenderer.getWidth(line) + 6;
            HudLayoutManager.begin(ctx, HudLayoutManager.REACH_DISPLAY, w, 14);
            ctx.drawTextWithShadow(mc.textRenderer, line, 3, 2, 0xFF888888);
            HudLayoutManager.end(ctx);
            return;
        }
        double dist;
        String kind;
        if (hit instanceof EntityHitResult eh) {
            dist = mc.player.getEyePos().distanceTo(eh.getEntity().getBoundingBox().getCenter());
            kind = "Entity";
        } else if (hit instanceof BlockHitResult bh) {
            dist = Math.sqrt(bh.squaredDistanceTo(mc.player));
            kind = "Block";
        } else return;
        String line = String.format("%s: %.2f m", kind, dist);
        int w = mc.textRenderer.getWidth(line) + 6;
        HudLayoutManager.begin(ctx, HudLayoutManager.REACH_DISPLAY, w, 14);
        ctx.drawTextWithShadow(mc.textRenderer, line, 3, 2, color.get() | 0xFF000000);
        HudLayoutManager.end(ctx);
    }
}
