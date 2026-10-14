package com.devs.client.module;

import com.devs.client.net.DevsPeerPayload;
import com.devs.client.net.PeerTracker;
import com.devs.client.render.HighlightRenderer;
import net.minecraft.entity.player.PlayerEntity;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Cosméticos del jugador local: china hat, trail animado, halo, anillos, orbes. */
public final class Cosmetics extends Module {
    private record Trail(Vec3d pos, int born) {}
    private record Orb(Vec3d pos, int born, float phase) {}

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final BooleanSetting chinaHat = add(new BooleanSetting("China Hat", true));
    public final BooleanSetting trail = add(new BooleanSetting("Trail", true));
    public final BooleanSetting halo = add(new BooleanSetting("Halo", false));
    public final BooleanSetting rings = add(new BooleanSetting("Foot Rings", false));
    public final BooleanSetting orbs = add(new BooleanSetting("Orbit Orbs", false));
    public final BooleanSetting mahoraga = add(new BooleanSetting("Mahoraga Wheel", false));
    public final NumberSetting mahaRadius = add(new NumberSetting("Wheel Size", 0.55, 0.25, 1.5, 0.05));
    public final NumberSetting mahaWidth = add(new NumberSetting("Wheel Line Width", 2.0, 0.5, 6.0, 0.25));
    public final NumberSetting mahaHeadOffset = add(new NumberSetting("Wheel Head Offset", 0.35, -0.2, 1.0, 0.05));
    public final BooleanSetting mahaSpinOnHit = add(new BooleanSetting("Wheel Spin On Hit", false));
    public final NumberSetting mahaSpinDuration = add(new NumberSetting("Spin Duration Ticks", 40, 10, 120, 5));

    public final NumberSetting trailLen = add(new NumberSetting("Trail Length", 32, 8, 80, 2));
    public final ModeSetting trailMode = add(new ModeSetting("Trail Mode", "Fade", "Fade", "Rainbow", "Pulse", "Spark"));
    public final NumberSetting trailWidth = add(new NumberSetting("Trail Width", 2.5, 1.0, 5.0, 0.5));
    public final NumberSetting trailWave = add(new NumberSetting("Trail Wave", 0.12, 0.0, 0.4, 0.02));
    public final NumberSetting hatRadius = add(new NumberSetting("Hat Radius", 0.55, 0.3, 1.2, 0.05));
    public final NumberSetting spinSpeed = add(new NumberSetting("Spin Speed", 1.0, 0.2, 4.0, 0.1));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF4DD2FF));
    public final BooleanSetting rainbow = add(new BooleanSetting("Rainbow Accent", false));

    private final List<Trail> points = new ArrayList<>();
    private final List<Orb> sparkOrbs = new ArrayList<>();
    private int tick;
    private float mahaAngle;
    private float lastHealth = -1f;
    private int mahaSpinTicks;

    public Cosmetics() {
        super("Cosmetics", "Hats, animated trails, halo and orbit effects on yourself.", "Render");
    }

    @Override protected void onDisable() {
        points.clear();
        sparkOrbs.clear();
        lastHealth = -1f;
        mahaSpinTicks = 0;
    }

    @Override public void tick() {
        tick++;
        if (!enabled() || mc.player == null) {
            points.clear();
            sparkOrbs.clear();
            return;
        }
        if (trail.get()) {
            Vec3d p = mc.player.getEntityPos().add(0, 0.12, 0);
            points.add(new Trail(p, tick));
            while (points.size() > trailLen.get().intValue()) points.remove(0);

            // spark particles along trail head
            if ("Spark".equals(trailMode.get()) && tick % 2 == 0) {
                double a = Math.random() * Math.PI * 2;
                double r = 0.15 + Math.random() * 0.25;
                sparkOrbs.add(new Orb(
                    p.add(Math.cos(a) * r, 0.05 + Math.random() * 0.4, Math.sin(a) * r),
                    tick, (float) Math.random()));
            }
        } else {
            points.clear();
        }
        sparkOrbs.removeIf(o -> tick - o.born() > 18);

        // Mahoraga: detectar golpe y controlar giro
        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (lastHealth >= 0f && hp < lastHealth - 0.01f) {
            mahaSpinTicks = mahaSpinDuration.get().intValue();
        }
        lastHealth = hp;
        if (mahaSpinTicks > 0) mahaSpinTicks--;

        boolean spinning = !mahaSpinOnHit.get() || mahaSpinTicks > 0;
        if (spinning) {
            mahaAngle += 0.05f * spinSpeed.get().floatValue();
        }
    }

    @Override public void render() {
        if (mc.player == null || mc.world == null) return;
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        float time = tick + tp;

        // Local
        Vec3d pos = mc.player.getLerpedPos(tp);
        int base = accentColor(time);
        float height = mc.player.getHeight();
        if (chinaHat.get()) renderChinaHat(pos, height, base, time);
        if (halo.get()) renderHalo(pos, height, base, time);
        if (rings.get()) renderFootRings(pos, height, base, time);
        if (orbs.get()) renderOrbitOrbs(pos, height, base, time);
        if (mahoraga.get()) renderMahoraga(pos, height, base, time);
        if (trail.get() && points.size() > 1) renderTrail(base, time);
        renderSparks(base, time);

        // Remotos (otros con Devs Client)
        for (PlayerEntity other : mc.world.getPlayers()) {
            if (other == mc.player) continue;
            var state = PeerTracker.get(other.getUuid());
            if (state == null || !state.has(DevsPeerPayload.FLAG_ENABLED)) continue;
            Vec3d op = other.getLerpedPos(tp);
            float oh = other.getHeight();
            int col = state.color() | 0xFF000000;
            if (state.has(DevsPeerPayload.FLAG_CHINA_HAT)) renderChinaHat(op, oh, col, time);
            if (state.has(DevsPeerPayload.FLAG_HALO)) renderHalo(op, oh, col, time);
            if (state.has(DevsPeerPayload.FLAG_RINGS)) renderFootRings(op, oh, col, time);
            if (state.has(DevsPeerPayload.FLAG_ORBS)) renderOrbitOrbs(op, oh, col, time);
            if (state.has(DevsPeerPayload.FLAG_MAHORAGA)) renderMahoraga(op, oh, col, time);
        }
    }

    private int accentColor(float time) {
        if (rainbow.get() || "Rainbow".equals(trailMode.get())) {
            float h = (time * 6f) % 360f;
            return hsv(h, 0.85f, 1f) | 0xFF000000;
        }
        return color.get() | 0xFF000000;
    }

    private void renderChinaHat(Vec3d pos, float playerHeight, int col, float time) {
        double r = hatRadius.get();
        double y = pos.y + playerHeight + 0.15;
        // gentle bob
        y += Math.sin(time * 0.12) * 0.03;
        double spin = time * 0.04 * spinSpeed.get();
        Vec3d tip = new Vec3d(pos.x, y + 0.38 + Math.sin(time * 0.15) * 0.02, pos.z);
        int segs = 20;
        Vec3d prev = null;
        for (int i = 0; i <= segs; i++) {
            double a = i * Math.PI * 2 / segs + spin;
            // slight wavy rim
            double rr = r * (1.0 + 0.04 * Math.sin(a * 3 + time * 0.2));
            Vec3d rim = new Vec3d(pos.x + Math.cos(a) * rr, y, pos.z + Math.sin(a) * rr);
            int c = rainbow.get() ? (hsv((float) ((a / Math.PI * 180) + time * 4) % 360, 0.8f, 1f) | 0xFF000000) : col;
            HighlightRenderer.line(tip, rim, HighlightRenderer.withOpacity(c, 0.85f), 1.6f);
            if (prev != null) HighlightRenderer.line(prev, rim, HighlightRenderer.withOpacity(c, 0.7f), 1.3f);
            prev = rim;
        }
        // small floating jewel on tip
        double jr = 0.06 + 0.02 * Math.sin(time * 0.25);
        HighlightRenderer.drawBox(
            new net.minecraft.util.math.Box(tip.x - jr, tip.y - jr, tip.z - jr, tip.x + jr, tip.y + jr, tip.z + jr),
            com.devs.client.render.HighlightMode.FILLED,
            HighlightRenderer.withOpacity(col, 0.9f), 1f);
    }

    private void renderHalo(Vec3d pos, float playerHeight, int col, float time) {
        double y = pos.y + playerHeight + 0.35 + Math.sin(time * 0.1) * 0.04;
        double r = 0.35;
        int segs = 24;
        double spin = time * 0.08 * spinSpeed.get();
        Vec3d prev = null;
        for (int i = 0; i <= segs; i++) {
            double a = i * Math.PI * 2 / segs + spin;
            Vec3d p = new Vec3d(pos.x + Math.cos(a) * r, y + Math.sin(a * 2 + time * 0.15) * 0.03, pos.z + Math.sin(a) * r);
            if (prev != null) {
                int c = rainbow.get() ? (hsv((float) (i * 15 + time * 3) % 360, 0.7f, 1f) | 0xFF000000) : col;
                HighlightRenderer.line(prev, p, HighlightRenderer.withOpacity(c, 0.8f), 2f);
            }
            prev = p;
        }
    }

    private void renderFootRings(Vec3d pos, float playerHeight, int col, float time) {
        // expanding pulse rings at feet
        for (int k = 0; k < 3; k++) {
            float phase = (time * 0.08f + k * 0.33f) % 1f;
            double r = 0.25 + phase * 0.9;
            float alpha = (1f - phase) * 0.7f;
            int segs = 18;
            Vec3d prev = null;
            double y = pos.y + 0.05;
            for (int i = 0; i <= segs; i++) {
                double a = i * Math.PI * 2 / segs;
                Vec3d p = new Vec3d(pos.x + Math.cos(a) * r, y, pos.z + Math.sin(a) * r);
                if (prev != null)
                    HighlightRenderer.line(prev, p, HighlightRenderer.withOpacity(col, alpha), 1.5f);
                prev = p;
            }
        }
    }

    private void renderOrbitOrbs(Vec3d pos, float playerHeight, int col, float time) {
        int n = 4;
        double baseR = 0.7;
        double cy = pos.y + playerHeight * 0.55;
        for (int i = 0; i < n; i++) {
            double a = time * 0.1 * spinSpeed.get() + i * (Math.PI * 2 / n);
            double elev = Math.sin(time * 0.12 + i) * 0.25;
            Vec3d p = new Vec3d(
                pos.x + Math.cos(a) * baseR,
                cy + elev,
                pos.z + Math.sin(a) * baseR);
            double s = 0.07 + 0.02 * Math.sin(time * 0.3 + i);
            int c = rainbow.get() ? (hsv((float) (i * 90 + time * 5) % 360, 0.85f, 1f) | 0xFF000000) : col;
            HighlightRenderer.drawBox(
                new net.minecraft.util.math.Box(p.x - s, p.y - s, p.z - s, p.x + s, p.y + s, p.z + s),
                com.devs.client.render.HighlightMode.OUTLINE_FILLED,
                HighlightRenderer.withOpacity(c, 0.85f), 1.2f);
            // thin tether to body
            HighlightRenderer.line(new Vec3d(pos.x, cy, pos.z), p, HighlightRenderer.withOpacity(c, 0.25f), 1f);
        }
    }


    /**
     * Rueda de Mahoraga (timón / dharma wheel):
     * aro exterior, aro interior, 8 radios, 8 manijas con bolas.
     */
    private void renderMahoraga(Vec3d pos, float playerHeight, int col, float time) {
        // En la cabeza del jugador
        double cx = pos.x;
        double cy = pos.y + playerHeight + mahaHeadOffset.get() + Math.sin(time * 0.08) * 0.02;
        double cz = pos.z;
        double outerR = mahaRadius.get();
        double innerR = outerR * 0.55;
        double hubR = outerR * 0.14;
        double handleLen = outerR * 0.28;
        double knobR = outerR * 0.09;
        float lw = mahaWidth.get().floatValue();
        double spin = mahaAngle;
        int segs = 32;
        int spokes = 8;

        Vec3d prev = null;
        for (int i = 0; i <= segs; i++) {
            double a = i * Math.PI * 2 / segs + spin;
            Vec3d pt = new Vec3d(cx + Math.cos(a) * outerR, cy, cz + Math.sin(a) * outerR);
            if (prev != null)
                HighlightRenderer.line(prev, pt, HighlightRenderer.withOpacity(col, 0.95f), lw);
            prev = pt;
        }
        prev = null;
        for (int i = 0; i <= segs; i++) {
            double a = i * Math.PI * 2 / segs + spin;
            Vec3d pt = new Vec3d(cx + Math.cos(a) * innerR, cy, cz + Math.sin(a) * innerR);
            if (prev != null)
                HighlightRenderer.line(prev, pt, HighlightRenderer.withOpacity(col, 0.85f), lw * 0.85f);
            prev = pt;
        }
        prev = null;
        for (int i = 0; i <= 16; i++) {
            double a = i * Math.PI * 2 / 16 + spin;
            Vec3d pt = new Vec3d(cx + Math.cos(a) * hubR, cy, cz + Math.sin(a) * hubR);
            if (prev != null)
                HighlightRenderer.line(prev, pt, HighlightRenderer.withOpacity(col, 0.9f), lw * 0.9f);
            prev = pt;
        }
        double hr = hubR * 0.45;
        HighlightRenderer.drawBox(
            new net.minecraft.util.math.Box(cx - hr, cy - hr * 0.4, cz - hr, cx + hr, cy + hr * 0.4, cz + hr),
            com.devs.client.render.HighlightMode.FILLED,
            HighlightRenderer.withOpacity(col, 0.95f), 1f);

        for (int i = 0; i < spokes; i++) {
            double a = i * Math.PI * 2 / spokes + spin;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            Vec3d from = new Vec3d(cx + cos * hubR, cy, cz + sin * hubR);
            Vec3d toRim = new Vec3d(cx + cos * outerR, cy, cz + sin * outerR);
            HighlightRenderer.line(from, toRim, HighlightRenderer.withOpacity(col, 0.9f), lw * 0.9f);
            Vec3d handleEnd = new Vec3d(cx + cos * (outerR + handleLen), cy, cz + sin * (outerR + handleLen));
            HighlightRenderer.line(toRim, handleEnd, HighlightRenderer.withOpacity(col, 0.95f), lw);
            Vec3d prevK = null;
            for (int k = 0; k <= 12; k++) {
                double ka = k * Math.PI * 2 / 12;
                Vec3d kp = new Vec3d(handleEnd.x + Math.cos(ka) * knobR, cy, handleEnd.z + Math.sin(ka) * knobR);
                if (prevK != null)
                    HighlightRenderer.line(prevK, kp, HighlightRenderer.withOpacity(col, 0.95f), lw * 0.85f);
                prevK = kp;
            }
            double kr = knobR * 0.35;
            HighlightRenderer.drawBox(
                new net.minecraft.util.math.Box(handleEnd.x - kr, cy - kr * 0.5, handleEnd.z - kr,
                    handleEnd.x + kr, cy + kr * 0.5, handleEnd.z + kr),
                com.devs.client.render.HighlightMode.FILLED,
                HighlightRenderer.withOpacity(col, 0.9f), 1f);
        }
    }

    private void renderTrail(int baseCol, float time) {
        float wave = trailWave.get().floatValue();
        float width = trailWidth.get().floatValue();
        String mode = trailMode.get();
        int n = points.size();

        for (int i = 1; i < n; i++) {
            float t = i / (float) n; // 0 = tail, 1 = head
            Trail a = points.get(i - 1);
            Trail b = points.get(i);

            // wave offset perpendicular-ish (vertical bob)
            double bob = Math.sin(time * 0.25 + i * 0.4) * wave * (1.0 - t * 0.3);
            Vec3d from = a.pos().add(0, bob, 0);
            Vec3d to = b.pos().add(0, Math.sin(time * 0.25 + (i + 1) * 0.4) * wave * (1.0 - (i + 1) / (float) n * 0.3), 0);

            float alpha;
            int col;
            float lw = width * (0.4f + t * 0.8f);

            switch (mode) {
                case "Rainbow" -> {
                    col = hsv((time * 8f + i * 8f) % 360f, 0.85f, 1f) | 0xFF000000;
                    alpha = 0.2f + t * 0.8f;
                }
                case "Pulse" -> {
                    float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.35 + i * 0.2);
                    col = baseCol;
                    alpha = (0.15f + t * 0.75f) * (0.55f + pulse * 0.45f);
                    lw *= 0.8f + pulse * 0.5f;
                }
                case "Spark" -> {
                    col = baseCol;
                    alpha = 0.15f + t * 0.7f;
                }
                default -> { // Fade
                    col = baseCol;
                    alpha = 0.12f + t * 0.88f;
                }
            }

            HighlightRenderer.line(from, to, HighlightRenderer.withOpacity(col, alpha), lw);

            // soft secondary thinner line for glow
            if (t > 0.5f) {
                HighlightRenderer.line(from, to, HighlightRenderer.withOpacity(col, alpha * 0.35f), lw + 1.5f);
            }
        }
    }

    private void renderSparks(int col, float time) {
        if (sparkOrbs.isEmpty()) return;
        for (Orb o : sparkOrbs) {
            float age = (tick - o.born()) + (time - tick);
            float life = 18f;
            if (age > life) continue;
            float fade = 1f - age / life;
            double rise = age * 0.03;
            Vec3d p = o.pos().add(0, rise, 0);
            double s = 0.04 * fade;
            HighlightRenderer.drawBox(
                new net.minecraft.util.math.Box(p.x - s, p.y - s, p.z - s, p.x + s, p.y + s, p.z + s),
                com.devs.client.render.HighlightMode.FILLED,
                HighlightRenderer.withOpacity(col, fade * 0.9f), 1f);
        }
    }

    /** HSV → RGB (h in degrees). */
    private static int hsv(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f;
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        int ri = MathHelper.clamp((int) ((r + m) * 255), 0, 255);
        int gi = MathHelper.clamp((int) ((g + m) * 255), 0, 255);
        int bi = MathHelper.clamp((int) ((b + m) * 255), 0, 255);
        return (ri << 16) | (gi << 8) | bi;
    }
}
