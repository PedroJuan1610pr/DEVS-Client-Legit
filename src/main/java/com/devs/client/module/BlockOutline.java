package com.devs.client.module;

import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

/** Outline custom del bloque bajo la mira (no es X-ray). */
public final class BlockOutline extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final ModeSetting mode = add(new ModeSetting("Mode", "Outline", "Outline", "Filled", "Outline + Filled", "Corners"));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.45, 0.1, 1.0, 0.05));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 2.0, 1.0, 5.0, 0.5));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF4DD2FF));

    public BlockOutline() { super("Block Outline", "Custom outline for the block you look at.", "Render"); }

    @Override public void render() {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = bh.getBlockPos();
        var state = mc.world.getBlockState(pos);
        if (state.isAir()) return;
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) return;
        Box box = shape.getBoundingBox().offset(pos.getX(), pos.getY(), pos.getZ());
        boolean prev = HighlightRenderer.throughWalls;
        HighlightRenderer.throughWalls = false;
        try {
            HighlightRenderer.drawBox(box, HighlightRenderer.parseMode(mode.get()),
                HighlightRenderer.withOpacity(color.get(), opacity.get().floatValue()),
                lineWidth.get().floatValue());
        } finally {
            HighlightRenderer.throughWalls = prev;
        }
    }
}
