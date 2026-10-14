package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Cuenta items de hotbar/inventario (obsidian, crystals…). */
public final class BlockCounter extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final ItemListSetting items = add(new ItemListSetting(
        "Items", "obsidian,end_crystal,ender_chest,totem_of_undying,experience_bottle,golden_apple,enchanted_golden_apple"));
    public final ModeSetting display = add(new ModeSetting("Display", "Icons", "Icons", "Text"));
    public final BooleanSetting hotbarOnly = add(new BooleanSetting("Hotbar Only", false));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));

    public BlockCounter() {
        super("Block Counter", "Counts key items in inventory (obby, crystals…).", "Misc");
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        var track = items.toItemSet();
        if (track.isEmpty()) return;
        record Row(Item item, int count) {}
        List<Row> rows = new ArrayList<>();
        var inv = mc.player.getInventory();
        int limit = hotbarOnly.get() ? 9 : inv.size();
        for (Item it : track) {
            int c = 0;
            for (int i = 0; i < limit; i++) {
                ItemStack st = inv.getStack(i);
                if (st.is(it)) c += st.getCount();
            }
            // offhand
            if (!hotbarOnly.get() && mc.player.getOffHandStack().is(it))
                c += mc.player.getOffHandStack().getCount();
            if (c > 0) rows.add(new Row(it, c));
        }
        if (rows.isEmpty()) return;
        var tr = mc.textRenderer;
        boolean icons = "Icons".equals(display.get());
        int w = 8, h = 4;
        if (icons) {
            w = rows.size() * 20 + 4;
            h = 22;
        } else {
            for (Row r : rows) {
                String name = Registries.ITEM.getId(r.item()).getPath();
                w = Math.max(w, tr.getWidth(name + " x" + r.count()) + 8);
                h += 10;
            }
        }
        HudLayoutManager.begin(ctx, HudLayoutManager.BLOCK_COUNTER, w, h);
        if (background.get()) ctx.fill(0, 0, w, h, 0x90101018);
        if (icons) {
            int x = 2;
            for (Row r : rows) {
                ItemStack st = new ItemStack(r.item(), r.count());
                ctx.drawItem(st, x, 2);
                ctx.drawStackOverlay(tr, st, x, 2);
                x += 20;
            }
        } else {
            int y = 2;
            for (Row r : rows) {
                String name = Registries.ITEM.getId(r.item()).getPath();
                ctx.drawTextWithShadow(tr, name + " x" + r.count(), 4, y, 0xFFFFFFFF);
                y += 10;
            }
        }
        HudLayoutManager.end(ctx);
    }
}
