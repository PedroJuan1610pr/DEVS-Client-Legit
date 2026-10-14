package com.devs.client.module;

import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Dibuja las hitboxes reales de las entidades, con altura de ojos y rayo de mirada opcionales. */
public final class Hitboxes extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting mode = add(new ModeSetting("Mode", "Outline", "Outline", "Filled", "Outline + Filled", "Corners"));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.35, 0.05, 1.0, 0.05));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 2.0, 1.0, 6.0, 0.5));
    public final NumberSetting range = add(new NumberSetting("Range", 64, 8, 256, 8));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting hostile = add(new BooleanSetting("Hostile Mobs", true));
    public final BooleanSetting otherMobs = add(new BooleanSetting("Other Mobs", false));
    public final BooleanSetting projectiles = add(new BooleanSetting("Projectiles", false));
    public final BooleanSetting self = add(new BooleanSetting("Self", false));
    public final BooleanSetting eyeLevel = add(new BooleanSetting("Eye Level", true));
    public final BooleanSetting lookRay = add(new BooleanSetting("Look Ray", false));
    public final NumberSetting rayLength = add(new NumberSetting("Ray Length", 3.0, 1.0, 6.0, 0.5));
    public final ColorSetting playerColor = add(new ColorSetting("Player Color", 0xFF4DD2FF));
    public final ColorSetting hostileColor = add(new ColorSetting("Hostile Color", 0xFFFF4D6D));
    public final ColorSetting otherColor = add(new ColorSetting("Other Color", 0xFF6FE3A1));
    public final ColorSetting projectileColor = add(new ColorSetting("Projectile Color", 0xFFFFD94D));

    public Hitboxes() { super("Hitboxes", "Draws the real hitbox of nearby entities (not through walls).", "Render"); }

    @Override public void render() {
        if (mc.world == null || mc.player == null) return;
        HighlightMode hm = HighlightRenderer.parseMode(mode.get());
        float a = opacity.get().floatValue(), lw = lineWidth.get().floatValue();
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        double r = range.get(), r2 = r * r;

        // Legit: hitboxes only visible when not occluded by blocks
        boolean prev = HighlightRenderer.throughWalls;
        HighlightRenderer.throughWalls = false;
        try {
        for (Entity e : mc.world.getEntities()) {
            if (e.isRemoved() || e.isSpectator()) continue;
            if (e == mc.player && !self.get()) continue;
            int color;
            if (e instanceof PlayerEntity) { if (!players.get()) continue; color = playerColor.get(); }
            else if (e instanceof Monster) { if (!hostile.get()) continue; color = hostileColor.get(); }
            else if (e instanceof LivingEntity) { if (!otherMobs.get()) continue; color = otherColor.get(); }
            else if (e instanceof ProjectileEntity) { if (!projectiles.get()) continue; color = projectileColor.get(); }
            else continue;
            if (e.squaredDistanceTo(mc.player) > r2) continue;

            Vec3d lerped = e.getLerpedPos(tp);
            Box box = e.getBoundingBox().offset(lerped.subtract(e.getEntityPos()));
            HighlightRenderer.drawBox(box, hm, HighlightRenderer.withOpacity(color, a), lw);

            double eyeY = box.minY + (e.getEyeY() - e.getY());
            if (eyeLevel.get() && e instanceof LivingEntity) {
                Box eye = new Box(box.minX, eyeY - 0.01, box.minZ, box.maxX, eyeY + 0.01, box.maxZ);
                HighlightRenderer.drawBox(eye, HighlightMode.OUTLINE, HighlightRenderer.withOpacity(0xFFFF4D6D, 0.9f), lw);
            }
            if (lookRay.get() && e instanceof LivingEntity) {
                Vec3d eye = new Vec3d(lerped.x, eyeY, lerped.z);
                Vec3d dir = Vec3d.fromPolar(e.getPitch(), e.getHeadYaw());
                HighlightRenderer.line(eye, eye.add(dir.multiply(rayLength.get())), HighlightRenderer.withOpacity(color, 0.9f), lw);
            }
        }
        } finally {
            HighlightRenderer.throughWalls = prev;
        }
    }
}
