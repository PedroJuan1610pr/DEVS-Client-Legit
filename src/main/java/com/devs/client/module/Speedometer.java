package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

/** Velocidad horizontal en bloques/segundo. */
public final class Speedometer extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting vertical = add(new BooleanSetting("Include Vertical", false));
    public final BooleanSetting kmh = add(new BooleanSetting("Show km/h", false));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF6FE3A1));

    private Vec3d last;
    private double speed;

    public Speedometer() { super("Speedometer", "Shows your current speed in blocks/s.", "Misc"); }

    @Override public void tick() {
        if (mc.player == null) { last = null; speed = 0; return; }
        Vec3d p = mc.player.getEntityPos();
        if (last != null) {
            double dx = p.x - last.x, dy = p.y - last.y, dz = p.z - last.z;
            double d = vertical.get() ? Math.sqrt(dx*dx+dy*dy+dz*dz) : Math.sqrt(dx*dx+dz*dz);
            speed = d * 20.0; // per second
        }
        last = p;
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        var tr = mc.textRenderer;
        String line = kmh.get()
            ? String.format("Speed: %.1f km/h", speed * 3.6 * 0.2) // rough aesthetic; real is b/s * 3.6 if 1 block=1m
            : String.format("Speed: %.2f b/s", speed);
        // Prefer honest b/s; if kmh: treat 1 block = 1 meter
        if (kmh.get()) line = String.format("Speed: %.1f km/h", speed * 3.6);
        int w = tr.getWidth(line) + 6;
        HudLayoutManager.begin(ctx, HudLayoutManager.SPEEDOMETER, w, 14);
        ctx.drawTextWithShadow(tr, line, 3, 2, color.get() | 0xFF000000);
        HudLayoutManager.end(ctx);
    }
}
