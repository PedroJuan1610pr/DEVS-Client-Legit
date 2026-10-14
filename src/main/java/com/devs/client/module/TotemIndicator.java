package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;

/** Indicador de tótems en inventario / offhand. */
public final class TotemIndicator extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting warnBelow = add(new NumberSetting("Warn Below", 1, 0, 10, 1));
    public final BooleanSetting showIcon = add(new BooleanSetting("Show Icon", true));
    public final ColorSetting okColor = add(new ColorSetting("OK Color", 0xFF6FE3A1));
    public final ColorSetting lowColor = add(new ColorSetting("Low Color", 0xFFFF4D6D));

    public TotemIndicator() { super("Totem Indicator", "Shows totem count and offhand status.", "Misc"); }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        int tots = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).is(Items.TOTEM_OF_UNDYING)) tots += inv.getStack(i).getCount();
        }
        boolean off = mc.player.getOffHandStack().is(Items.TOTEM_OF_UNDYING);
        boolean low = tots <= warnBelow.get().intValue();
        String line = "Totems: " + tots + (off ? " (offhand)" : "");
        int w = mc.textRenderer.getWidth(line) + (showIcon.get() ? 22 : 6);
        HudLayoutManager.begin(ctx, HudLayoutManager.TOTEM_INDICATOR, w, 18);
        int x = 3;
        if (showIcon.get()) {
            ctx.drawItem(Items.TOTEM_OF_UNDYING.getDefaultStack(), x, 1);
            x += 18;
        }
        ctx.drawTextWithShadow(mc.textRenderer, line, x, 4, (low ? lowColor.get() : okColor.get()) | 0xFF000000);
        HudLayoutManager.end(ctx);
    }
}
