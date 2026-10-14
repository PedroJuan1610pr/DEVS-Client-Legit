package com.devs.client.module;

import com.devs.client.McCompat;

import com.devs.client.DevsClient;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Etiquetas sobre entidades (estilo Autism + Screen tags DEVS):
 * jugadores/mobs/items, vida, distancia, equipo, agrupacion de items, oculta nametags vanilla.
 * Etiquetas sobre las entidades: nombre, pops, vida con barra, distancia y equipo.
 * Style "Screen" (por defecto): se dibujan en 2D sobre la pantalla con fondo. Su tamano depende de la
 * distancia (perspectiva: mas lejos = mas pequeno), controlado por Distance Scaling / Reference Distance /
 * Min Scale / Max Scale. Style "World": las etiquetas flotantes de texto en el mundo de antes.
 */
public final class Nametags extends Module {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int PAD = 4;

    private record Tag(LivingEntity e, double dist, float sx, float sy) {}
    /** Las lejanas primero para que las cercanas queden dibujadas encima. */
    private static final Comparator<Tag> FAR_FIRST = Comparator.comparingDouble(Tag::dist).reversed();

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Reutilizados cada frame para no generar basura (se llama a renderHud a 60+ FPS).
    private final List<Tag> tags = new ArrayList<>();
    private final List<ItemStack> icons = new ArrayList<>();
    private final float[] out = new float[2];
    private final String[] labels = new String[8];
    private final int[] cellW = new int[8];
    // Base de la camara, calculada una sola vez por frame (antes se recalculaba por entidad).
    private double cx, cy, cz, fx, fy, fz, rx, rz, ux, uy, uz, tanHalfFov, aspect;

    public final ModeSetting style = add(new ModeSetting("Style", "Screen", "Screen", "World"));
    public final NumberSetting range = add(new NumberSetting("Range", 96, 16, 256, 8));
    /** Multiplicador global del tamano (1.0 = tamano base a la Reference Distance). */
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 3.0, 0.1));
    /** 0 = tamano fijo; 1 = perspectiva real (el tag se encoge exactamente como el jugador). */
    public final NumberSetting distanceScaling = add(new NumberSetting("Distance Scaling", 0.75, 0.0, 1.0, 0.05));
    /** A esta distancia (bloques) el tag mide exactamente "Scale". Mas cerca = mayor, mas lejos = menor. */
    public final NumberSetting refDistance = add(new NumberSetting("Reference Distance", 8, 2, 32, 1));
    /** Limites del factor de distancia, para que ni desaparezca de lejos ni tape la pantalla de cerca. */
    public final NumberSetting minScale = add(new NumberSetting("Min Scale", 0.3, 0.1, 1.0, 0.05));
    public final NumberSetting maxScale = add(new NumberSetting("Max Scale", 2.0, 1.0, 4.0, 0.1));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting hostile = add(new BooleanSetting("Hostile Mobs", false));
    public final BooleanSetting otherMobs = add(new BooleanSetting("Other Mobs", false));
    public final BooleanSetting items = add(new BooleanSetting("Items", true));
    public final BooleanSetting groupItems = add(new BooleanSetting("Group Items", true));
    public final NumberSetting groupRadius = add(new NumberSetting("Group Radius", 5.0, 0.5, 16.0, 0.5));
    public final BooleanSetting hideVanilla = add(new BooleanSetting("Hide Vanilla Tags", true));
    public final BooleanSetting health = add(new BooleanSetting("Health", true));
    public final BooleanSetting healthColor = add(new BooleanSetting("Health Color", true));
    public final BooleanSetting distance = add(new BooleanSetting("Distance", true));
    public final BooleanSetting armor = add(new BooleanSetting("Armor", true));
    /** Off | Abbrev (PIV UIII) | Full (Protection IV) | Vertical (uno por linea, todos). */
    public final ModeSetting enchantStyle = add(new ModeSetting("Enchant Style", "Off", "Off", "Abbrev", "Full", "Vertical"));
    /** Tamano de los iconos de armadura/mano (1.0 = 16 px a escala del tag). */
    public final NumberSetting armorScale = add(new NumberSetting("Armor Scale", 1.5, 0.8, 3.0, 0.1));
    /** Como se muestra la durabilidad bajo cada pieza: Off / Bar / Percent / Exact / Bar + Percent / Bar + Exact. */
    public final ModeSetting durability = add(new ModeSetting("Durability", "Bar", Durability.MODES));
    public final BooleanSetting heldItem = add(new BooleanSetting("Held Item", true));
    /** Above = encima (clasico). Left/Right = panel al costado del jugador (estilo CS). */
    public final ModeSetting placement = add(new ModeSetting("Placement", "Above", "Above", "Left", "Right"));
    public final NumberSetting sideGap = add(new NumberSetting("Side Gap", 28, 8, 80, 1));
    public final ColorSetting nameColor = add(new ColorSetting("Name Color", 0xFFFFFFFF));
    public final BooleanSetting pops = add(new BooleanSetting("Totem Pops", true));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", true));

    public Nametags() { super("Nametags", "Name, health, distance and gear above entities.", "Render"); }

    private boolean screenStyle() { return "Screen".equals(style.get()); }

    /** Usado por EntityNameTagMixin para ocultar el nametag vanilla. */
    public static boolean shouldSuppressVanilla(Entity e) {
        Nametags n = instance();
        if (n == null || !n.enabled() || !n.hideVanilla.get()) return false;
        return n.accepts(e) || (e instanceof ItemEntity && n.items.get());
    }

    private static Nametags instance() {
        try {
            for (Module m : com.devs.client.DevsClient.MODULES.all()) {
                if (m instanceof Nametags nt) return nt;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private boolean accepts(Entity e) {
        if (!(e instanceof LivingEntity le) || le.isRemoved() || le.isSpectator()) return false;
        if (e == mc.player) return false;
        if (e instanceof PlayerEntity) return players.get();
        if (e instanceof Monster) return hostile.get();
        return otherMobs.get();
    }

    private int hpColor(LivingEntity le) {
        float ratio = le.getHealth() / Math.max(1f, le.getMaxHealth());
        return ratio > 0.66f ? 0xFF6FE3A1 : ratio > 0.33f ? 0xFFFFD94D : 0xFFFF4D6D;
    }

    // ---------------------------------------------------------------- Screen style (HUD)

    /** Factor de tamano segun la distancia: (ref / dist)^k acotado a [Min, Max], por el multiplicador Scale. */
    private float tagScale(double dist) {
        double k = distanceScaling.get();
        double f = 1.0;
        if (k > 0) f = Math.pow(Math.max(dist, 0.5) / refDistance.get(), -k);
        f = Math.max(minScale.get(), Math.min(maxScale.get(), f));
        return (float) (f * scale.get());
    }

    /** Un decimal sin String.format (mas barato y sin depender del idioma: siempre "12.5", nunca "12,5"). */
    private static String one(float v) { return Double.toString(Math.round(v * 10.0) / 10.0); }

    @Override public void renderHud(DrawContext ctx) {
        if (!screenStyle() || mc.world == null || mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cpos = cam.getCameraPos();
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        double r2 = range.get() * range.get();
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        setupCamera(cam, cpos, sw, sh);

        tags.clear();
        for (Entity e : mc.world.getEntities()) {
            if (!accepts(e)) continue;
            double d2 = e.squaredDistanceTo(cpos);
            if (d2 > r2) continue;
            LivingEntity le = (LivingEntity) e;
            if (!throughWalls.get() && e != mc.player && !mc.player.canSee(le)) continue;
            Vec3d lp = e.getLerpedPos(tp);
            boolean side = !"Above".equals(placement.get());
            double py = side ? lp.y + e.getHeight() * 0.55 : lp.y + e.getHeight() + 0.3;
            if (!project(lp.x, py, lp.z, sw, sh)) continue;
            tags.add(new Tag(le, Math.sqrt(d2), out[0], out[1]));
        }
        tags.sort(FAR_FIRST);
        for (Tag t : tags) drawTag(ctx, t, sw, sh);
        tags.clear();   // no retener referencias a entidades entre frames
        if (items.get() && screenStyle()) renderItemTags(ctx, cam, cpos, tp, r2, sw, sh);
    }

    private void renderItemTags(DrawContext ctx, Camera cam, Vec3d cpos, float tp, double r2, int sw, int sh) {
        List<ItemEntity> list = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity ie) || ie.isRemoved()) continue;
            if (e.squaredDistanceTo(cpos) > r2) continue;
            list.add(ie);
        }
        if (list.isEmpty()) return;
        boolean[] visited = new boolean[list.size()];
        double gr2 = groupRadius.get() * groupRadius.get();
        var tr = mc.textRenderer;
        for (int i = 0; i < list.size(); i++) {
            if (visited[i]) continue;
            ItemEntity base = list.get(i);
            visited[i] = true;
            List<ItemEntity> cluster = new ArrayList<>();
            cluster.add(base);
            if (groupItems.get()) {
                for (int j = i + 1; j < list.size(); j++) {
                    if (visited[j]) continue;
                    if (base.squaredDistanceTo(list.get(j)) <= gr2) {
                        visited[j] = true;
                        cluster.add(list.get(j));
                    }
                }
            }
            Map<String, int[]> agg = new LinkedHashMap<>(); // name -> [count]
            double cx = 0, cy = 0, cz = 0;
            for (ItemEntity ie : cluster) {
                ItemStack st = ie.getStack();
                String name = st.getName().getString();
                agg.computeIfAbsent(name, k -> new int[]{0})[0] += st.getCount();
                var lp = ie.getLerpedPos(tp);
                cx += lp.x; cy += lp.y; cz += lp.z;
            }
            int n = cluster.size();
            cx /= n; cy = cy / n + 0.5; cz /= n;
            double dist = Math.sqrt((cx - cpos.x) * (cx - cpos.x) + (cy - cpos.y) * (cy - cpos.y) + (cz - cpos.z) * (cz - cpos.z));
            if (!project(cx, cy, cz, sw, sh)) continue;

            List<Map.Entry<String, int[]>> entries = new ArrayList<>(agg.entrySet());
            entries.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
            List<String> lines = new ArrayList<>();
            int max = Math.min(entries.size(), 6);
            for (int k = 0; k < max; k++) {
                var e = entries.get(k);
                lines.add(e.getKey() + " x" + e.getValue()[0]);
            }
            if (entries.size() > max) lines.add("+" + (entries.size() - max) + " more");

            float s = tagScale(dist);
            float sx = Math.round(out[0]), sy = Math.round(out[1]);
            int lw = 0;
            for (String l : lines) lw = Math.max(lw, tr.getWidth(l));
            int w = lw + PAD * 2, h = lines.size() * 10 + PAD * 2;
            var mt = ctx.getMatrices();
            mt.pushMatrix();
            mt.translate(sx, sy);
            mt.scale(s, s);
            int x0 = -w / 2, y0 = -h - 2;
            if (background.get()) {
                ctx.fill(x0, y0, x0 + w, y0 + h, 0xC0101018);
                ctx.fill(x0, y0, x0 + w, y0 + 1, 0xFFFFD94D);
            }
            int ty = y0 + PAD;
            for (String l : lines) {
                ctx.drawTextWithShadow(tr, l, x0 + PAD, ty, 0xFFFFFFFF);
                ty += 10;
            }
            mt.popMatrix();
        }
    }

    private void drawTag(DrawContext ctx, Tag t, int sw, int sh) {
        LivingEntity le = t.e();
        var tr = mc.textRenderer;
        String place = placement.get();
        boolean side = "Left".equals(place) || "Right".equals(place);

        String name = le.getName().getString();
        String popsText = "";
        if (pops.get() && le instanceof PlayerEntity pl) {
            int n = DevsClient.TOTEM_POPS.popsOf(pl.getUuid());
            if (n > 0) popsText = " [" + n + (n == 1 ? " pop]" : " pops]");
        }
        float hp = le.getHealth(), abs = le.getAbsorptionAmount();
        String hpText = health.get() ? one(hp) + (abs > 0 ? " +" + one(abs) : "") : "";
        String distText = distance.get() ? Math.round(t.dist()) + "m" : "";

        icons.clear();
        int armorCount = 0;
        if (armor.get()) for (EquipmentSlot s : ARMOR) {
            ItemStack st = McCompat.equipped(le, s);
            if (!st.isEmpty()) { icons.add(st); armorCount++; }
        }
        if (heldItem.get()) {
            ItemStack st = le.getMainHandStack();
            if (!st.isEmpty()) icons.add(st);
        }

        String dMode = durability.get();
        boolean dBar = Durability.hasBar(dMode), dText = Durability.hasText(dMode);
        float as = armorScale.get().floatValue();
        int iconSize = Math.round(16 * as);
        float ts = Math.max(0.5f, as * 0.55f);
        int barH = Math.max(2, Math.round(as * 1.5f));
        int textH = Math.round(8 * ts);
        int gap = Math.max(2, Math.round(as * 2));
        String enchMode = enchantStyle.get();
        boolean showEnch = armor.get() && !"Off".equals(enchMode);
        boolean enchVert = "Vertical".equals(enchMode);
        boolean enchFull = "Full".equals(enchMode);
        float enchTs = Math.max(0.4f, as * (enchVert || enchFull ? 0.5f : 0.42f));
        int enchLineH = Math.round(8 * enchTs);

        @SuppressWarnings("unchecked")
        List<String>[] enchLines = new List[Math.max(icons.size(), 1)];
        boolean anyBar = false, anyText = false, anyEnch = false;

        // anchos/altos por pieza (para layout horizontal Above)
        int iconsW = 0;
        // layout vertical (side): cada fila = icon + optional text/ench to the right
        int sideRowW = 0;
        int sideStackH = 0;

        for (int i = 0; i < icons.size(); i++) {
            ItemStack st = icons.get(i);
            boolean dmg = st.isDamageable();
            int cw = iconSize;
            String lb = null;
            if (dmg && dText) {
                lb = Durability.text(st, dMode);
                cw = Math.max(cw, (int) Math.ceil(tr.getWidth(lb) * ts));
                anyText = true;
            }
            if (dmg && dBar) anyBar = true;

            List<String> elines = List.of();
            int enchW = 0, enchH = 0;
            if (showEnch && i < armorCount) {
                elines = enchantLines(st, enchFull, enchVert);
                if (!elines.isEmpty()) {
                    anyEnch = true;
                    for (String el : elines) {
                        enchW = Math.max(enchW, (int) Math.ceil(tr.getWidth(el) * enchTs));
                    }
                    enchH = enchVert ? elines.size() * enchLineH : enchLineH;
                    if (!side) cw = Math.max(cw, enchW);
                }
            }
            enchLines[i] = elines;
            labels[i] = lb;
            cellW[i] = cw;
            iconsW += cw + (i > 0 ? gap : 0);

            // side row: icon | dur text / enchants
            int textColW = 0;
            if (lb != null) textColW = Math.max(textColW, (int) Math.ceil(tr.getWidth(lb) * ts));
            textColW = Math.max(textColW, enchW);
            int rowW = iconSize + (dBar ? 3 + barH : 0) + (textColW > 0 ? 3 + textColW : 0);
            int rowH = Math.max(iconSize, Math.max(dBar ? iconSize : 0, enchH > 0 ? enchH : (lb != null ? textH : 0)));
            sideRowW = Math.max(sideRowW, rowW);
            sideStackH += rowH + (i > 0 ? gap : 0);
        }

        int nameW = tr.getWidth(name);
        int hpW = hpText.isEmpty() ? 0 : tr.getWidth(hpText);
        int distW = distText.isEmpty() ? 0 : tr.getWidth(distText);
        int popsW = popsText.isEmpty() ? 0 : tr.getWidth(popsText);

        int w, h, x0, y0;
        int accent = healthColor.get() ? hpColor(le) : 0xFF4DD2FF;
        float s = tagScale(t.dist());
        float sx = Math.round(t.sx()), sy = Math.round(t.sy());

        // offset fuera de la hitbox: half-width del jugador en pantalla + Side Gap
        float halfBodyPx = (float) ((le.getWidth() * 0.55) / Math.max(0.6, t.dist())
            / Math.max(0.001, tanHalfFov * aspect) * (sw * 0.5));
        int gapSide = sideGap.get().intValue() + Math.round(halfBodyPx) + 6;

        final int VHP_W = 4; // barra de vida vertical

        if (side) {
            // Panel vertical compacto: nombre, hp num, dist, armadura en columna, vida vertical al borde
            int textBlockW = Math.max(nameW + popsW, Math.max(hpW, distW));
            int contentW = Math.max(textBlockW, sideRowW);
            int vhpPad = health.get() ? VHP_W + 3 : 0;
            w = contentW + PAD * 2 + vhpPad;
            int headerH = 10 + (hpW > 0 && !health.get() ? 0 : 0);
            // name + optional hp text line + dist + armor stack
            h = PAD * 2 + 10; // name
            if (!hpText.isEmpty()) h += 10;
            if (health.get()) { /* barra vertical no suma altura extra del header */ }
            if (!distText.isEmpty()) h += 10;
            if (!icons.isEmpty()) h += sideStackH + 2;
            h = Math.max(h, health.get() ? 40 : h);

            if ("Right".equals(place)) {
                x0 = gapSide;
                y0 = -h / 2;
            } else {
                x0 = -w - gapSide;
                y0 = -h / 2;
            }
        } else {
            int enchBlockH = 0;
            if (anyEnch) {
                int maxEnchLines = 0;
                for (int i = 0; i < icons.size(); i++) {
                    if (enchLines[i] != null) maxEnchLines = Math.max(maxEnchLines, enchLines[i].size());
                }
                enchBlockH = enchVert ? maxEnchLines * enchLineH + 1 : enchLineH + 1;
            }
            int iconsH = icons.isEmpty() ? 0 : iconSize
                + (anyBar ? barH + 1 : 0)
                + (anyText ? textH + 1 : 0)
                + enchBlockH;
            int row1 = nameW + popsW + (hpW == 0 ? 0 : 10 + hpW);
            int inner = Math.max(Math.max(row1, distW), Math.max(iconsW, 56));
            w = inner + PAD * 2;
            h = PAD * 2 + 10 + (health.get() ? 5 : 0) + (distText.isEmpty() ? 0 : 10)
                + (icons.isEmpty() ? 0 : iconsH + 3);
            x0 = -w / 2;
            y0 = -h - 2;
            gapSide = 0;
        }

        if (sx < -w * s - 60 || sx > sw + w * s + 60 || sy < -h * s - 20 || sy > sh + h * s + 20) return;

        var mt = ctx.getMatrices();
        mt.pushMatrix();
        mt.translate(sx, sy);
        mt.scale(s, s);

        if (background.get()) {
            ctx.fill(x0, y0, x0 + w, y0 + h, 0xC0101018);
            if ("Right".equals(place)) {
                ctx.fill(x0, y0, x0 + 1, y0 + h, accent);
            } else if ("Left".equals(place)) {
                ctx.fill(x0 + w - 1, y0, x0 + w, y0 + h, accent);
            } else {
                ctx.fill(x0, y0, x0 + w, y0 + 1, accent);
                ctx.fill(x0, y0 + h - 1, x0 + w, y0 + h, 0xFF000000);
            }
        }

        int nc = nameColor.get() | 0xFF000000;

        if (side) {
            // ---- SIDE LAYOUT (CS-style) ----
            boolean right = "Right".equals(place);
            int vhpX = right ? x0 + PAD : x0 + w - PAD - VHP_W;
            int contentX = right ? x0 + PAD + (health.get() ? VHP_W + 3 : 0) : x0 + PAD;
            int contentRight = right ? x0 + w - PAD : x0 + w - PAD - (health.get() ? VHP_W + 3 : 0);

            int y = y0 + PAD;
            ctx.drawTextWithShadow(tr, name, contentX, y, nc);
            if (!popsText.isEmpty())
                ctx.drawTextWithShadow(tr, popsText, contentX + nameW, y, 0xFFFFD94D);
            y += 10;

            if (!hpText.isEmpty()) {
                ctx.drawTextWithShadow(tr, hpText, contentX, y, accent);
                y += 10;
            }
            if (!distText.isEmpty()) {
                ctx.drawTextWithShadow(tr, distText, contentX, y, 0xFFB8B8C4);
                y += 10;
            }

            // barra de vida VERTICAL (altura del contenido de armadura o del panel)
            if (health.get()) {
                float maxHp = Math.max(1f, le.getMaxHealth());
                float ratio = MathHelper.clamp(hp / maxHp, 0f, 1f);
                int barTop = y0 + PAD;
                int barBot = y0 + h - PAD;
                int barHgt = Math.max(12, barBot - barTop);
                ctx.fill(vhpX, barTop, vhpX + VHP_W, barTop + barHgt, 0xFF2A2A33);
                int filled = Math.round(barHgt * ratio);
                ctx.fill(vhpX, barTop + barHgt - filled, vhpX + VHP_W, barTop + barHgt, accent);
                if (abs > 0) {
                    int ab = Math.min(barHgt, Math.round(barHgt * abs / maxHp));
                    ctx.fill(vhpX, barTop + barHgt - filled - ab, vhpX + VHP_W, barTop + barHgt - filled, 0xFFFFC94D);
                }
            }

            // armadura VERTICAL: casco → pecho → piernas → botas → mano
            int iy = y + 2;
            for (int i = 0; i < icons.size(); i++) {
                ItemStack st = icons.get(i);
                int icx = contentX;
                mt.pushMatrix();
                mt.translate(icx, iy);
                mt.scale(as, as);
                ctx.drawItem(st, 0, 0);
                if (!st.isDamageable()) ctx.drawStackOverlay(tr, st, 0, 0);
                mt.popMatrix();

                // barra de durabilidad vertical al lado del icono
                int nextX = icx + iconSize + 2;
                if (dBar && st.isDamageable()) {
                    float maxD = Math.max(1, st.getMaxDamage());
                    float left = (maxD - st.getDamage()) / maxD;
                    int bh = iconSize;
                    int bw = Math.max(2, barH);
                    ctx.fill(nextX, iy, nextX + bw, iy + bh, 0xFF2A2A33);
                    int fh = Math.round(bh * left);
                    int col = left > 0.6f ? 0xFF6FE3A1 : left > 0.3f ? 0xFFFFD94D : 0xFFFF4D6D;
                    ctx.fill(nextX, iy + bh - fh, nextX + bw, iy + bh, col);
                    nextX += bw + 2;
                }

                int ty = iy;
                if (labels[i] != null) {
                    mt.pushMatrix();
                    mt.translate(nextX, ty);
                    mt.scale(ts, ts);
                    ctx.drawTextWithShadow(tr, labels[i], 0, 0, Durability.textColor(st, 15));
                    mt.popMatrix();
                    ty += textH;
                }
                List<String> elines = enchLines[i];
                if (elines != null) {
                    for (String el : elines) {
                        mt.pushMatrix();
                        mt.translate(nextX, ty);
                        mt.scale(enchTs, enchTs);
                        ctx.drawTextWithShadow(tr, el, 0, 0, 0xFFB8A0FF);
                        mt.popMatrix();
                        ty += enchLineH;
                    }
                }

                int rowH = Math.max(iconSize, ty - iy);
                iy += rowH + gap;
            }
        } else {
            // ---- ABOVE (classic horizontal) ----
            int x = x0 + PAD, y = y0 + PAD;
            int row1 = nameW + popsW + (hpW == 0 ? 0 : 10 + hpW);
            int inner = Math.max(Math.max(row1, distW), Math.max(iconsW, 56));

            ctx.drawTextWithShadow(tr, name, x, y, nc);
            if (!popsText.isEmpty()) ctx.drawTextWithShadow(tr, popsText, x + nameW, y, 0xFFFFD94D);
            if (hpW > 0) ctx.drawTextWithShadow(tr, hpText, x0 + w - PAD - hpW, y, accent);
            y += 10;

            if (health.get()) {
                float maxHp = Math.max(1f, le.getMaxHealth());
                float ratio = MathHelper.clamp(hp / maxHp, 0f, 1f);
                ctx.fill(x, y, x + inner, y + 3, 0xFF2A2A33);
                ctx.fill(x, y, x + Math.round(inner * ratio), y + 3, accent);
                if (abs > 0) ctx.fill(x, y + 3, x + Math.min(inner, Math.round(inner * abs / maxHp)), y + 4, 0xFFFFC94D);
                y += 5;
            }
            if (distW > 0) {
                ctx.drawTextWithShadow(tr, distText, x0 + (w - distW) / 2, y, 0xFFB8B8C4);
                y += 10;
            }
            if (!icons.isEmpty()) {
                int enchBlockH = 0;
                if (anyEnch) {
                    int maxEnchLines = 0;
                    for (int i = 0; i < icons.size(); i++) {
                        if (enchLines[i] != null) maxEnchLines = Math.max(maxEnchLines, enchLines[i].size());
                    }
                    enchBlockH = enchVert ? maxEnchLines * enchLineH + 1 : enchLineH + 1;
                }
                int ix = x0 + (w - iconsW) / 2;
                int barY = y + iconSize + 1;
                int textY = barY + (anyBar ? barH + 1 : 0);
                int enchY = textY + (anyText ? textH + 1 : 0);
                if (!anyText && anyBar) enchY = barY + barH + 1;
                if (!anyText && !anyBar) enchY = y + iconSize + 1;

                for (int i = 0; i < icons.size(); i++) {
                    ItemStack st = icons.get(i);
                    int cw = cellW[i];
                    int icx = ix + (cw - iconSize) / 2;
                    mt.pushMatrix();
                    mt.translate(icx, y);
                    mt.scale(as, as);
                    ctx.drawItem(st, 0, 0);
                    if (!st.isDamageable()) ctx.drawStackOverlay(tr, st, 0, 0);
                    mt.popMatrix();
                    if (dBar && st.isDamageable()) Durability.drawBar(ctx, icx, barY, iconSize, barH, st);
                    if (labels[i] != null) {
                        mt.pushMatrix();
                        mt.translate(Math.round(ix + (cw - tr.getWidth(labels[i]) * ts) / 2f), textY);
                        mt.scale(ts, ts);
                        ctx.drawTextWithShadow(tr, labels[i], 0, 0, Durability.textColor(st, 15));
                        mt.popMatrix();
                    }
                    List<String> elines = enchLines[i];
                    if (elines != null && !elines.isEmpty()) {
                        int ey = enchY;
                        for (String el : elines) {
                            mt.pushMatrix();
                            mt.translate(Math.round(ix + (cw - tr.getWidth(el) * enchTs) / 2f), ey);
                            mt.scale(enchTs, enchTs);
                            ctx.drawTextWithShadow(tr, el, 0, 0, 0xFFB8A0FF);
                            mt.popMatrix();
                            ey += enchLineH;
                        }
                    }
                    ix += cw + gap;
                }
            }
        }
        mt.popMatrix();
    }

    /** Calcula una vez por frame la base de la camara (adelante/derecha/arriba) y los parametros de proyeccion. */
    private void setupCamera(Camera cam, Vec3d c, int w, int h) {
        cx = c.x; cy = c.y; cz = c.z;
        double yaw = Math.toRadians(cam.getYaw()), pitch = Math.toRadians(cam.getPitch());
        double cyw = Math.cos(yaw), syw = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        fx = -syw * cp; fy = -sp; fz = cyw * cp;           // adelante
        rx = -cyw; rz = -syw;                              // derecha (ry = 0)
        ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;   // arriba = derecha x adelante
        tanHalfFov = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        aspect = (double) w / h;
    }

    /** Proyecta un punto del mundo a coordenadas de pantalla (GUI escalada) y lo deja en {@link #out}. */
    private boolean project(double px, double py, double pz, int w, int h) {
        double dx = px - cx, dy = py - cy, dz = pz - cz;
        double z = dx * fx + dy * fy + dz * fz;
        if (z < 0.05) return false;                        // detras de la camara
        double x = dx * rx + dz * rz;
        double y = dx * ux + dy * uy + dz * uz;
        out[0] = (float) (w / 2.0 + (x / (z * tanHalfFov * aspect)) * (w / 2.0));
        out[1] = (float) (h / 2.0 - (y / (z * tanHalfFov)) * (h / 2.0));
        return true;
    }

    // ---------------------------------------------------------------- World style (texto flotante)

    @Override public void render() {
        if (screenStyle() || mc.world == null || mc.player == null) return;
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        double r2 = range.get() * range.get();

        for (Entity e : mc.world.getEntities()) {
            if (!accepts(e) || e == mc.player) continue;
            LivingEntity le = (LivingEntity) e;
            double d2 = e.squaredDistanceTo(mc.player);
            if (d2 > r2) continue;
            double dist = Math.sqrt(d2);

            float hp = le.getHealth(), abs = le.getAbsorptionAmount();
            int color = health.get() && healthColor.get() ? hpColor(le) : (nameColor.get() | 0xFF000000);
            String line1 = e.getName().getString();
            if (health.get()) line1 += "  " + one(hp) + " HP" + (abs > 0 ? " (+" + one(abs) + ")" : "");
            if (pops.get() && e instanceof PlayerEntity pp) {
                int n = DevsClient.TOTEM_POPS.popsOf(pp.getUuid());
                if (n > 0) line1 += "  [" + n + (n == 1 ? " pop]" : " pops]");
            }

            List<String> parts = new ArrayList<>();
            if (distance.get()) parts.add(Math.round(dist) + "m");
            if (armor.get() && le.getArmor() > 0) parts.add("Armor " + le.getArmor());
            if (heldItem.get()) {
                ItemStack st = le.getMainHandStack();
                if (!st.isEmpty()) parts.add(st.getName().getString());
            }

            Vec3d lp = e.getLerpedPos(tp);
            float s = HighlightRenderer.labelScale(dist) * scale.get().floatValue();
            double base = lp.y + e.getHeight() + 0.35;
            HighlightRenderer.label(line1, new Vec3d(lp.x, base + (parts.isEmpty() ? 0 : 0.3 * s), lp.z), color, s);
            if (!parts.isEmpty()) {
                HighlightRenderer.label(String.join("  |  ", parts), new Vec3d(lp.x, base, lp.z), 0xFFCCCCCC, s * 0.8f);
            }
        }
    }


    /**
     * Lista de enchants de una pieza.
     * @param full true = nombre completo; false = abreviatura
     * @param vertical true = una linea por enchant (todos); false = una sola linea concatenada
     */
    private static List<String> enchantLines(ItemStack st, boolean full, boolean vertical) {
        ItemEnchantmentsComponent enc = getEnchants(st);
        if (enc == null || enc.getSize() == 0) return List.of();
        List<String> parts = new ArrayList<>();
        for (var entry : enc.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> e = entry.getKey();
            int lvl = entry.getIntValue();
            String name = full ? fullEnchant(e) : shortEnchant(e);
            if (name == null) continue;
            String line = lvl > 1 ? name + " " + toRoman(lvl) : name;
            // Abbrev compacto: PIV sin espacio
            if (!full && !vertical) line = lvl > 1 ? name + toRoman(lvl) : name;
            parts.add(line);
        }
        if (parts.isEmpty()) return List.of();
        if (vertical) return parts;
        // horizontal: una sola linea
        return List.of(String.join(full ? ", " : " ", parts));
    }

    private static ItemEnchantmentsComponent getEnchants(ItemStack st) {
        if (st == null || st.isEmpty()) return null;
        try {
            ItemEnchantmentsComponent enc = st.get(DataComponentTypes.ENCHANTMENTS);
            if (enc != null && enc.getSize() > 0) return enc;
        } catch (Throwable ignored) {}
        try {
            return st.get(DataComponentTypes.STORED_ENCHANTMENTS);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String fullEnchant(RegistryEntry<Enchantment> e) {
        try {
            Identifier id = e.getKey().map(k -> k.getValue()).orElse(null);
            String path = id != null ? id.getPath() : "enchant";
            // "fire_protection" -> "Fire Protection"
            String[] parts = path.split("_");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (p.isEmpty()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "Enchant";
        }
    }

    private static String shortEnchant(RegistryEntry<Enchantment> e) {
        try {
            Identifier id = e.getKey().map(k -> k.getValue()).orElse(null);
            String path = id != null ? id.getPath() : e.value().toString();
            return switch (path) {
                case "protection" -> "P";
                case "fire_protection" -> "FP";
                case "feather_falling" -> "FF";
                case "blast_protection" -> "BP";
                case "projectile_protection" -> "PP";
                case "respiration" -> "R";
                case "aqua_affinity" -> "AA";
                case "thorns" -> "Th";
                case "depth_strider" -> "DS";
                case "frost_walker" -> "FW";
                case "soul_speed" -> "SS";
                case "swift_sneak" -> "Sn";
                case "sharpness" -> "Sh";
                case "smite" -> "Sm";
                case "bane_of_arthropods" -> "BoA";
                case "knockback" -> "Kb";
                case "fire_aspect" -> "FA";
                case "looting" -> "L";
                case "sweeping_edge" -> "Sw";
                case "efficiency" -> "Eff";
                case "silk_touch" -> "ST";
                case "unbreaking" -> "U";
                case "fortune" -> "F";
                case "power" -> "Pow";
                case "punch" -> "Pu";
                case "flame" -> "Fl";
                case "infinity" -> "Inf";
                case "luck_of_the_sea" -> "LS";
                case "lure" -> "Lu";
                case "loyalty" -> "Lo";
                case "impaling" -> "Imp";
                case "riptide" -> "Rip";
                case "channeling" -> "Ch";
                case "multishot" -> "MS";
                case "quick_charge" -> "QC";
                case "piercing" -> "Pi";
                case "mending" -> "M";
                case "binding_curse" -> "Bind";
                case "vanishing_curse" -> "Van";
                case "density" -> "Den";
                case "breach" -> "Br";
                case "wind_burst" -> "WB";
                default -> {
                    String[] parts = path.split("_");
                    if (parts.length >= 2)
                        yield ("" + Character.toUpperCase(parts[0].charAt(0)) + Character.toUpperCase(parts[1].charAt(0)));
                    yield path.length() <= 3 ? path : path.substring(0, 3);
                }
            };
        } catch (Throwable t) {
            return "?";
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

}
