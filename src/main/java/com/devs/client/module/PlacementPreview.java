package com.devs.client.module;

import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Preview del bloque que vas a colocar (sin auto-place). */
public final class PlacementPreview extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.35, 0.1, 0.8, 0.05));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF4DD2FF));
    public final ColorSetting blocked = add(new ColorSetting("Blocked Color", 0xFFFF4D6D));

    public PlacementPreview() { super("Placement Preview", "Shows where the held block would be placed.", "World"); }

    @Override public void render() {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)
            && !(mc.player.getOffHandStack().getItem() instanceof BlockItem)) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK) return;
        BlockPos place = bh.getBlockPos().offset(bh.getSide());
        boolean free = mc.world.getBlockState(place).isReplaceable();
        int col = free ? color.get() : blocked.get();
        boolean prev = HighlightRenderer.throughWalls;
        HighlightRenderer.throughWalls = false;
        try {
            HighlightRenderer.drawBox(new Box(place), HighlightMode.OUTLINE_FILLED,
                HighlightRenderer.withOpacity(col, opacity.get().floatValue()), 1.5f);
        } finally {
            HighlightRenderer.throughWalls = prev;
        }
    }
}
