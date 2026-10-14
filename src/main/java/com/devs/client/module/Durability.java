package com.devs.client.module;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/** Utilidades compartidas por Nametags y Armor HUD para mostrar la durabilidad (barra, porcentaje o valor exacto). */
public final class Durability {
    /** Modos disponibles para el ajuste "Durability". */
    public static final String[] MODES = {"Off", "Bar", "Percent", "Exact", "Bar + Percent", "Bar + Exact"};

    private Durability() {}

    public static boolean hasBar(String mode) { return mode.startsWith("Bar"); }
    public static boolean hasText(String mode) { return mode.endsWith("Percent") || mode.endsWith("Exact"); }

    /** Durabilidad restante (puntos de uso que le quedan al item). */
    public static int left(ItemStack st) { return Math.max(0, st.getMaxDamage() - st.getDamage()); }

    /** Fraccion restante entre 0 y 1. */
    public static float fraction(ItemStack st) {
        int max = Math.max(1, st.getMaxDamage());
        return MathHelper.clamp((max - st.getDamage()) / (float) max, 0f, 1f);
    }

    public static int percent(ItemStack st) { return Math.round(fraction(st) * 100f); }

    /** Texto segun el modo: "87%" o el valor exacto restante ("1766"). */
    public static String text(ItemStack st, String mode) {
        return mode.endsWith("Percent") ? percent(st) + "%" : Integer.toString(left(st));
    }

    public static int textColor(ItemStack st, int warnBelow) {
        int p = percent(st);
        return p <= warnBelow ? 0xFFFF4D6D : p <= 50 ? 0xFFFFD94D : 0xFF6FE3A1;
    }

    /** Verde -> amarillo -> rojo, igual que la barra vanilla. */
    public static int barColor(ItemStack st) {
        return 0xFF000000 | MathHelper.hsvToRgb(fraction(st) / 3f, 1f, 1f);
    }

    public static void drawBar(DrawContext ctx, int x, int y, int w, int h, ItemStack st) {
        ctx.fill(x, y, x + w, y + h, 0xFF2A2A33);
        int fw = Math.round(w * fraction(st));
        if (fw > 0) ctx.fill(x, y, x + fw, y + h, barColor(st));
    }
}
