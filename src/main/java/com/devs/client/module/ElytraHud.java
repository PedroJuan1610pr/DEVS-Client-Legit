package com.devs.client.module;

import com.devs.client.McCompat;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** HUD de elytra: pitch, velocidad, fireworks, ayuda de aterrizaje. */
public final class ElytraHud extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting onlyWhenFlying = add(new BooleanSetting("Only When Flying", true));
    public final BooleanSetting pitch = add(new BooleanSetting("Pitch", true));
    public final BooleanSetting speed = add(new BooleanSetting("Speed", true));
    public final BooleanSetting rockets = add(new BooleanSetting("Firework Count", true));
    public final BooleanSetting landing = add(new BooleanSetting("Landing Helper", true));
    public final NumberSetting lowRockets = add(new NumberSetting("Low Rocket Warn", 3, 0, 16, 1));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF4DD2FF));

    private Vec3d last;
    private double spd;

    public ElytraHud() { super("Elytra HUD", "Pitch, speed, fireworks and landing angle while gliding.", "Misc"); }

    @Override public void tick() {
        if (mc.player == null) { last = null; return; }
        Vec3d p = mc.player.getEntityPos();
        if (last != null) {
            double dx = p.x - last.x, dy = p.y - last.y, dz = p.z - last.z;
            spd = Math.sqrt(dx*dx+dy*dy+dz*dz) * 20.0;
        }
        last = p;
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        boolean flying = mc.player.isGliding();
        boolean hasElytra = McCompat.equipped(mc.player, EquipmentSlot.CHEST).is(Items.ELYTRA);
        if (onlyWhenFlying.get() && !flying) return;
        if (!hasElytra && !flying) return;

        var tr = mc.textRenderer;
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (pitch.get()) {
            float p = mc.player.getPitch();
            String hint = p > 40 ? "↓ dive" : p < -10 ? "↑ climb" : "→ glide";
            lines.add(String.format("Pitch: %.0f° %s", p, hint));
        }
        if (speed.get()) lines.add(String.format("Air speed: %.2f b/s", spd));
        if (rockets.get()) {
            int n = 0;
            var inv = mc.player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                if (inv.getStack(i).is(Items.FIREWORK_ROCKET)) n += inv.getStack(i).getCount();
            }
            String r = "Rockets: " + n;
            if (n <= lowRockets.get().intValue()) r += " !";
            lines.add(r);
        }
        if (landing.get() && flying) {
            float p = mc.player.getPitch();
            // gentle landing zone roughly 20-40 pitch looking down
            String land = p > 50 ? "Steep" : p > 25 ? "OK land" : p > 0 ? "Shallow" : "Climbing";
            lines.add("Landing: " + land);
        }
        if (lines.isEmpty()) return;
        int w = 0, lh = 10;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        HudLayoutManager.begin(ctx, HudLayoutManager.ELYTRA_HUD, w + 6, lines.size() * lh + 4);
        int y = 2;
        int col = color.get() | 0xFF000000;
        for (String l : lines) {
            int c = l.contains("!") ? 0xFFFF4D6D : col;
            ctx.drawTextWithShadow(tr, l, 3, y, c);
            y += lh;
        }
        HudLayoutManager.end(ctx);
    }
}
