package com.devs.client.module;

import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Aura / ondas cosméticas cuando estás quieto. */
public final class IdleAura extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting radius = add(new NumberSetting("Radius", 0.9, 0.4, 2.0, 0.05));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.2, 3.0, 0.1));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF4DD2FF));
    public final NumberSetting idleTicks = add(new NumberSetting("Idle After (ticks)", 40, 10, 100, 5));

    private int still;
    private int tick;
    private Vec3d last;

    public IdleAura() { super("Idle Aura", "Cosmetic rings when standing still.", "Render"); }

    @Override public void tick() {
        tick++;
        if (mc.player == null) { still = 0; last = null; return; }
        Vec3d p = mc.player.getEntityPos();
        if (last != null && p.squaredDistanceTo(last) < 0.0001) still++;
        else still = 0;
        last = p;
    }

    @Override public void render() {
        if (mc.player == null || still < idleTicks.get().intValue()) return;
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d pos = mc.player.getLerpedPos(tp);
        float time = tick + tp;
        int col = color.get();
        boolean prev = HighlightRenderer.throughWalls;
        HighlightRenderer.throughWalls = false;
        try {
            for (int k = 0; k < 3; k++) {
                float phase = ((time * 0.05f * speed.get().floatValue()) + k * 0.33f) % 1f;
                double r = radius.get() * (0.4 + phase * 0.9);
                float alpha = (1f - phase) * 0.55f;
                double y = pos.y + 0.05 + k * 0.02;
                Vec3d prevP = null;
                int segs = 28;
                for (int i = 0; i <= segs; i++) {
                    double a = i * Math.PI * 2 / segs;
                    Vec3d pt = new Vec3d(pos.x + Math.cos(a) * r, y, pos.z + Math.sin(a) * r);
                    if (prevP != null)
                        HighlightRenderer.line(prevP, pt, HighlightRenderer.withOpacity(col, alpha), 1.4f);
                    prevP = pt;
                }
            }
        } finally {
            HighlightRenderer.throughWalls = prev;
        }
    }
}
