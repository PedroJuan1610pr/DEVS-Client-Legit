package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lista de efectos activos con nivel y tiempo restante. Posicion/escala: HUD Editor. */
public final class PotionHud extends Module {
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting sort = add(new ModeSetting("Sort", "Time", "Time", "Name"));
    public final BooleanSetting showAmbient = add(new BooleanSetting("Show Beacon Effects", true));
    public final NumberSetting warnSeconds = add(new NumberSetting("Warn Below (s)", 10, 0, 60, 5));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));

    public PotionHud() { super("Potion HUD", "Active status effects with level and time left.", "Misc"); }

    private static String name(StatusEffectInstance e) {
        String n = e.getEffectType().value().getName().getString();
        int amp = e.getAmplifier();
        return amp <= 0 ? n : n + " " + (amp < ROMAN.length ? ROMAN[amp] : String.valueOf(amp + 1));
    }

    private static String time(StatusEffectInstance e) {
        if (e.isInfinite()) return "Inf";
        int s = Math.max(0, e.getDuration() / 20);
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        List<StatusEffectInstance> list = new ArrayList<>(mc.player.getStatusEffects());
        if (!showAmbient.get()) list.removeIf(StatusEffectInstance::isAmbient);
        if (list.isEmpty()) return;
        if (sort.get().equals("Name")) list.sort(Comparator.comparing(PotionHud::name));
        else list.sort(Comparator.comparingInt(e -> e.isInfinite() ? Integer.MAX_VALUE : e.getDuration()));

        int rowH = 12, nameW = 0, timeW = 0;
        for (StatusEffectInstance e : list) {
            nameW = Math.max(nameW, mc.textRenderer.getWidth(name(e)));
            timeW = Math.max(timeW, mc.textRenderer.getWidth(time(e)));
        }
        int panelW = 6 + nameW + 10 + timeW;
        int total = list.size() * rowH;
        int contentW = panelW + 6, contentH = total + 4;

        HudLayoutManager.begin(ctx, HudLayoutManager.POTION_HUD, contentW, contentH);
        int x = 3, y = 3;
        if (background.get()) ctx.fill(x - 3, y - 3, x + panelW + 3, y + total + 1, 0x90000000);

        boolean blink = (mc.player.age / 4) % 2 == 0;
        for (StatusEffectInstance e : list) {
            int fx = 0xFF000000 | e.getEffectType().value().getColor();
            ctx.fill(x, y + 1, x + 3, y + rowH - 2, fx);
            ctx.drawTextWithShadow(mc.textRenderer, name(e), x + 6, y + 1, 0xFFFFFFFF);
            boolean low = !e.isInfinite() && e.getDuration() <= warnSeconds.get() * 20 && blink;
            String t = time(e);
            ctx.drawTextWithShadow(mc.textRenderer, t, x + panelW - mc.textRenderer.getWidth(t), y + 1, low ? 0xFFFF4D6D : 0xFFCCCCCC);
            y += rowH;
        }
        HudLayoutManager.end(ctx);
    }
}
