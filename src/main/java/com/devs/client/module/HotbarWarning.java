package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/** Aviso de durabilidad baja / stack bajo en hotbar. */
public final class HotbarWarning extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final NumberSetting duraPct = add(new NumberSetting("Durability %", 15, 1, 50, 1));
    public final NumberSetting stackLow = add(new NumberSetting("Stack Low", 8, 1, 64, 1));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFF4D6D));

    public HotbarWarning() { super("Hotbar Warning", "Warns when hotbar items are low durability or count.", "Misc"); }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        java.util.List<String> warns = new java.util.ArrayList<>();
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack st = inv.getStack(i);
            if (st.isEmpty()) continue;
            String name = st.getName().getString();
            if (st.isDamageable()) {
                float left = 1f - (float) st.getDamage() / Math.max(1, st.getMaxDamage());
                if (left * 100f <= duraPct.get())
                    warns.add(String.format("%s %.0f%%", name, left * 100));
            } else if (st.isStackable() && st.getCount() <= stackLow.get().intValue()) {
                warns.add(name + " x" + st.getCount());
            }
        }
        if (warns.isEmpty()) return;
        var tr = mc.textRenderer;
        int w = 0, lh = 10;
        for (String l : warns) w = Math.max(w, tr.getWidth(l));
        String title = "Hotbar low:";
        w = Math.max(w, tr.getWidth(title));
        HudLayoutManager.begin(ctx, HudLayoutManager.HOTBAR_WARNING, w + 6, (warns.size() + 1) * lh + 4);
        int y = 2;
        ctx.drawTextWithShadow(tr, title, 3, y, color.get() | 0xFF000000);
        y += lh;
        for (String l : warns) { ctx.drawTextWithShadow(tr, l, 3, y, 0xFFFFAAAA); y += lh; }
        HudLayoutManager.end(ctx);
    }
}
