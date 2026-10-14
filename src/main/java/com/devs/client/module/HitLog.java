package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.BooleanSetting;
import com.devs.client.setting.ColorSetting;
import com.devs.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Muestra cuánto daño haces con cada golpe. El cliente no recibe el daño directamente, así que se
 * compara la vida (+absorción) del objetivo antes del golpe y cuando el servidor la actualiza.
 * Si el servidor oculta o falsea la vida de las entidades, los números no serán fiables.
 */
public final class HitLog extends Module {
    private static final int PENDING_TIMEOUT = 20;   // ticks esperando la actualización de vida
    private static final int HISTORY_TICKS = 100;    // cuánto dura una línea en la lista del HUD
    private static final int HISTORY_MAX = 5;

    private record Pending(LivingEntity target, float before, int tick) {}
    private record Popup(Vec3d pos, String text, int born) {}
    private record Line(String text, int born) {}

    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting floating = add(new BooleanSetting("Floating Text", true));
    public final BooleanSetting chat = add(new BooleanSetting("Chat", false));
    public final BooleanSetting hud = add(new BooleanSetting("HUD List", true));
    public final NumberSetting duration = add(new NumberSetting("Duration (s)", 1.5, 0.5, 5.0, 0.5));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFF4D6D));

    private final List<Pending> pending = new ArrayList<>();
    private final List<Popup> popups = new ArrayList<>();
    private final List<Line> history = new ArrayList<>();
    private int now;

    public HitLog() { super("Hit Log", "Shows the damage you deal with each hit.", "Combat"); }

    /** Llamado desde AttackHookMixin justo antes de enviar el golpe. */
    public void onAttack(Entity target) {
        if (!enabled() || !(target instanceof LivingEntity le)) return;
        for (Pending p : pending) if (p.target() == le) return;   // ya hay un golpe esperando resultado
        pending.add(new Pending(le, le.getHealth() + le.getAbsorptionAmount(), now));
    }

    @Override protected void onDisable() { pending.clear(); popups.clear(); history.clear(); }

    @Override public void tick() {
        now++;
        int life = (int) (duration.get() * 20);
        popups.removeIf(p -> now - p.born() > life);
        history.removeIf(l -> now - l.born() > HISTORY_TICKS);
        if (pending.isEmpty()) return;

        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            LivingEntity t = p.target();
            float cur = t.getHealth() + t.getAbsorptionAmount();
            float dmg = p.before() - cur;
            if (dmg > 0.01f) {
                report(t, dmg, Math.max(0f, cur));
                it.remove();
            } else if (t.isRemoved() || now - p.tick() > PENDING_TIMEOUT) {
                it.remove();   // golpe sin efecto visible (bloqueado, fallado, invulnerable...)
            }
        }
    }

    private void report(LivingEntity t, float dmg, float left) {
        String name = t.getName().getString();
        boolean kill = left <= 0f;
        String text = String.format("-%.1f", dmg) + (kill ? " KILL" : "");
        if (floating.get()) popups.add(new Popup(new Vec3d(t.getX(), t.getBoundingBox().maxY + 0.3, t.getZ()), text, now));
        history.add(new Line(name + "  " + text, now));
        while (history.size() > HISTORY_MAX) history.remove(0);
        if (chat.get() && mc.player != null) {
            mc.player.sendMessage(Text.literal(String.format("[DEVS] %s: -%.1f (%.1f left)", name, dmg, left))
                .formatted(kill ? Formatting.RED : Formatting.GOLD));
        }
    }

    @Override public void render() {
        if (!floating.get() || popups.isEmpty() || mc.player == null) return;
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        double life = Math.max(1, duration.get() * 20);
        for (Popup p : popups) {
            double age = (now - p.born()) + tp;
            if (age > life) continue;
            float fade = (float) (1.0 - age / life);
            Vec3d pos = p.pos().add(0, age * 0.03, 0);   // sube mientras se desvanece
            double dist = mc.player.getEyePos().distanceTo(pos);
            HighlightRenderer.label(p.text(), pos, HighlightRenderer.withOpacity(color.get(), Math.max(0.15f, fade)),
                HighlightRenderer.labelScale(dist));
        }
    }

    @Override public void renderHud(DrawContext ctx) {
        if (!hud.get() || history.isEmpty() || mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        int maxW = 0;
        List<String> lines = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0; i--) {   // la más reciente arriba
            String s = history.get(i).text();
            lines.add(s);
            maxW = Math.max(maxW, mc.textRenderer.getWidth(s));
        }
        int contentW = maxW + 4, contentH = lines.size() * 10 + 4;
        HudLayoutManager.begin(ctx, HudLayoutManager.HIT_LOG, contentW, contentH);
        int y = 2;
        for (String s : lines) {
            // Alineado a la derecha del panel (como el Hit Log original)
            ctx.drawTextWithShadow(mc.textRenderer, s, contentW - 2 - mc.textRenderer.getWidth(s), y, 0xFFFFFFFF);
            y += 10;
        }
        HudLayoutManager.end(ctx);
    }
}
