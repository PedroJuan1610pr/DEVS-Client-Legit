package com.devs.client.module;

import com.devs.client.setting.BooleanSetting;
import com.devs.client.setting.ColorSetting;
import com.devs.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Overlay estilo AppleSkin: la saturación se dibuja encima de los iconos de comida.
 */
public final class SaturationOverlay extends Module {
    public static SaturationOverlay INSTANCE;

    public final BooleanSetting enabledOverlay = add(new BooleanSetting("Show Overlay", true));
    public final NumberSetting alpha = add(new NumberSetting("Opacity", 0.75, 0.2, 1.0, 0.05));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFAA00)); // dorado
    public final BooleanSetting pulse = add(new BooleanSetting("Pulse", true));

    public SaturationOverlay() {
        super("Saturation Overlay", "AppleSkin-style saturation on the hunger bar.", "Render");
        INSTANCE = this;
    }

    /** Llamado desde InGameHudMixin tras renderFood(context, player, top, right). */
    public void renderOnFoodBar(DrawContext ctx, PlayerEntity player, int top, int right) {
        if (!enabled() || !enabledOverlay.get()) return;
        if (player == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        if (ScreenshotClean.shouldHideHud()) return;

        float sat = player.getHungerManager().getSaturationLevel();
        if (sat <= 0.01f) return;

        int rgb = color.get() & 0xFFFFFF;
        float a = alpha.get().floatValue();
        if (pulse.get()) {
            float t = (System.currentTimeMillis() % 1200L) / 1200f;
            a *= 0.75f + 0.25f * (float) Math.sin(t * Math.PI * 2);
        }
        int alphaI = Math.max(0, Math.min(255, (int) (a * 255))) << 24;
        int col = alphaI | rgb;

        // 10 iconos de 9x9, espaciados 8 px, anclados a la derecha (vanilla)
        for (int i = 0; i < 10; i++) {
            float iconLevel = sat / 2f - i; // cada icono = 2 pts de saturación
            if (iconLevel <= 0f) continue;

            int x = right - i * 8 - 9;
            int y = top;

            if (iconLevel >= 1f) {
                // icono completo: relleno suave tipo shank
                fillShank(ctx, x, y, col, 1f);
            } else {
                fillShank(ctx, x, y, col, iconLevel);
            }
        }
    }

    /** Dibuja un "drumstick" simplificado dentro del slot 9x9. */
    private static void fillShank(DrawContext ctx, int x, int y, int col, float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        // área útil ~7x7 centrada
        int w = 7;
        int fullH = 7;
        int h = Math.max(1, Math.round(fullH * fraction));
        int x0 = x + 1;
        int y0 = y + 1 + (fullH - h);
        ctx.fill(x0, y0, x0 + w, y0 + h, col);
        // borde más claro arriba para legibilidad
        int hi = (col & 0xFF000000) | 0xFFE8C060;
        if (h > 1) ctx.fill(x0, y0, x0 + w, y0 + 1, hi);
    }
}
