package com.devs.client.module;

import com.devs.client.DevsClient;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Números de daño flotantes grandes (hit bubbles). */
public final class DamageNumbers extends Module {
    private record Pending(LivingEntity target, float before, int tick) {}
    private record Bubble(Vec3d pos, String text, int born, int color) {}

    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting duration = add(new NumberSetting("Duration (s)", 1.2, 0.4, 3.0, 0.1));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFF4D6D));
    public final BooleanSetting critColor = add(new BooleanSetting("Crit Yellow", true));

    private final List<Pending> pending = new ArrayList<>();
    private final List<Bubble> bubbles = new ArrayList<>();
    private int now;

    public DamageNumbers() {
        super("Damage Numbers", "Floating damage numbers on hits.", "Combat");
    }

    public void onAttack(Entity target) {
        if (!enabled() || !(target instanceof LivingEntity le)) return;
        for (Pending p : pending) if (p.target() == le) return;
        pending.add(new Pending(le, le.getHealth() + le.getAbsorptionAmount(), now));
    }

    @Override protected void onDisable() { pending.clear(); bubbles.clear(); }

    @Override public void tick() {
        now++;
        int life = (int) (duration.get() * 20);
        bubbles.removeIf(b -> now - b.born() > life);
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            LivingEntity t = p.target();
            float cur = t.getHealth() + t.getAbsorptionAmount();
            float dmg = p.before() - cur;
            if (dmg > 0.05f) {
                int col = color.get() | 0xFF000000;
                if (critColor.get() && dmg >= 6f) col = 0xFFFFD94D;
                Vec3d pos = t.getEntityPos().add(0, t.getHeight() * 0.8, 0);
                bubbles.add(new Bubble(pos, String.format("%.1f", dmg), now, col));
                it.remove();
            } else if (t.isRemoved() || now - p.tick() > 20) it.remove();
        }
    }

    @Override public void render() {
        if (bubbles.isEmpty() || mc.player == null) return;
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        double life = Math.max(1, duration.get() * 20);
        for (Bubble b : bubbles) {
            double age = (now - b.born()) + tp;
            if (age > life) continue;
            float fade = (float) (1.0 - age / life);
            Vec3d pos = b.pos().add(0, age * 0.04, 0);
            double dist = mc.player.getEyePos().distanceTo(pos);
            HighlightRenderer.label(b.text(), pos,
                HighlightRenderer.withOpacity(b.color(), Math.max(0.2f, fade)),
                HighlightRenderer.labelScale(dist) * 1.3f);
        }
    }
}
