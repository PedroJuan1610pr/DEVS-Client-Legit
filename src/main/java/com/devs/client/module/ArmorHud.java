package com.devs.client.module;

import com.devs.client.McCompat;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Armadura y manos con durabilidad. Posicion/escala: HUD Editor. */
public final class ArmorHud extends Module {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting layout = add(new ModeSetting("Layout", "Vertical", "Vertical", "Horizontal"));
    /** Off / Bar / Percent / Exact / Bar + Percent / Bar + Exact. */
    public final ModeSetting durability = add(new ModeSetting("Durability", "Bar + Percent", Durability.MODES));
    public final BooleanSetting hands = add(new BooleanSetting("Show Hands", true));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final NumberSetting warnBelow = add(new NumberSetting("Warn Below %", 15, 0, 50, 5));

    public ArmorHud() { super("Armor HUD", "Armor and held items with durability on screen.", "Misc"); }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot s : ARMOR) { ItemStack st = McCompat.equipped(mc.player, s); if (!st.isEmpty()) stacks.add(st); }
        if (hands.get()) {
            ItemStack main = McCompat.equipped(mc.player, EquipmentSlot.MAINHAND), off = McCompat.equipped(mc.player, EquipmentSlot.OFFHAND);
            if (!main.isEmpty()) stacks.add(main);
            if (!off.isEmpty()) stacks.add(off);
        }
        if (stacks.isEmpty()) return;

        var tr = mc.textRenderer;
        String dm = durability.get();
        boolean dBar = Durability.hasBar(dm), dText = Durability.hasText(dm);
        int warn = warnBelow.get().intValue();

        List<String> texts = new ArrayList<>();
        int maxText = 0;
        boolean anyDmg = false;
        for (ItemStack st : stacks) {
            boolean dmg = st.isDamageable();
            anyDmg |= dmg;
            String t = dmg && dText ? Durability.text(st, dm) : "";
            texts.add(t);
            maxText = Math.max(maxText, tr.getWidth(t));
        }

        if (layout.get().equals("Horizontal")) {
            int cell = Math.max(20, maxText + 4);
            int extra = anyDmg ? (dBar ? 3 : 0) + (dText ? 10 : 0) : 0;
            int total = stacks.size() * cell;
            int contentW = total + 4, contentH = 19 + extra + 6;

            HudLayoutManager.begin(ctx, HudLayoutManager.ARMOR_HUD, contentW, contentH);
            int x = 2, y = 3;
            if (background.get()) ctx.fill(x - 2, y - 3, x + total + 2, y + 19 + extra, 0x90000000);
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack st = stacks.get(i);
                int ix = x + i * cell + (cell - 16) / 2;
                ctx.drawItem(st, ix, y);
                if (!st.isDamageable()) { ctx.drawStackOverlay(tr, st, ix, y); continue; }
                int yy = y + 17;
                if (dBar) { Durability.drawBar(ctx, ix, yy, 16, 2, st); yy += 3; }
                String t = texts.get(i);
                if (!t.isEmpty()) ctx.drawTextWithShadow(tr, t, x + i * cell + (cell - tr.getWidth(t)) / 2, yy + 1, Durability.textColor(st, warn));
            }
            HudLayoutManager.end(ctx);
            return;
        }

        // Vertical: icono + (texto y/o barra) a la derecha
        int rowH = 18;
        int colW = Math.max(dText ? maxText : 0, dBar && anyDmg ? 30 : 0);
        int panelW = 16 + (colW > 0 ? 4 + colW : 0);
        int contentW = panelW + 6, contentH = stacks.size() * rowH + 4;

        HudLayoutManager.begin(ctx, HudLayoutManager.ARMOR_HUD, contentW, contentH);
        int x0 = 3, y0 = 2;
        if (background.get()) {
            ctx.fill(x0 - 3, y0 - 2, x0 + panelW + 3, y0 + stacks.size() * rowH, 0x90000000);
            ctx.fill(x0 - 3, y0 - 2, x0 - 1, y0 + stacks.size() * rowH, 0xFF4DD2FF);
        }
        int y = y0;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack st = stacks.get(i);
            ctx.drawItem(st, x0, y);
            if (!st.isDamageable()) { ctx.drawStackOverlay(tr, st, x0, y); y += rowH; continue; }
            String t = texts.get(i);
            boolean hasT = !t.isEmpty();
            if (hasT) ctx.drawTextWithShadow(tr, t, x0 + 20, dBar ? y + 1 : y + 4, Durability.textColor(st, warn));
            if (dBar) Durability.drawBar(ctx, x0 + 20, hasT ? y + 11 : y + 6, colW, 3, st);
            y += rowH;
        }
        HudLayoutManager.end(ctx);
    }
}
