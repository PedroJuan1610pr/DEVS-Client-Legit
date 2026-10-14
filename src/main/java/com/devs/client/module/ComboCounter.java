package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;

/** Contador visual de combo (hits seguidos al mismo target). */
public final class ComboCounter extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting timeout = add(new NumberSetting("Timeout (s)", 1.5, 0.5, 5.0, 0.1));
    public final NumberSetting minShow = add(new NumberSetting("Min Combo", 2, 1, 10, 1));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFF4D6D));

    private int combo;
    private int lastTargetId = -1;
    private int ticksSinceHit;

    public ComboCounter() { super("Combo Counter", "Shows hit combo streak on the same target.", "Combat"); }

    public void onAttack(Entity target) {
        if (!enabled() || target == null) return;
        int id = target.getId();
        if (id == lastTargetId) combo++;
        else { combo = 1; lastTargetId = id; }
        ticksSinceHit = 0;
    }

    @Override public void tick() {
        if (combo <= 0) return;
        ticksSinceHit++;
        if (ticksSinceHit > timeout.get() * 20) { combo = 0; lastTargetId = -1; }
    }

    @Override public void renderHud(DrawContext ctx) {
        if (combo < minShow.get().intValue() || mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        String line = combo + "x Combo";
        int w = mc.textRenderer.getWidth(line) + 8;
        HudLayoutManager.begin(ctx, HudLayoutManager.COMBO_COUNTER, w, 16);
        ctx.drawTextWithShadow(mc.textRenderer, line, 4, 3, color.get() | 0xFF000000);
        HudLayoutManager.end(ctx);
    }
}
