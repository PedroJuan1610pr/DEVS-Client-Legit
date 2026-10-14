package com.devs.client.gui;

import com.devs.client.setting.ItemListSetting;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Editor de lista de items estilo Autism: busqueda + filas con icono.
 * Clic en un item = anadir/quitar de la lista del Consumable Counter.
 */
public final class ItemListEditorScreen extends Screen {
    private static final int PANEL_W = 440, PANEL_H = 340;
    private static final int HEAD = 22, SEARCH_H = 18, ROW_H = 26, ICON = 18;
    private static final int BG = 0xCC050507, PANEL = 0xF00B0C10, HEADER = 0xF0181A20;
    private static final int ROW = 0xB012141A, ROW_HOV = 0xC91D222B, ROW_ON = 0xC9281A1A;
    private static final int TEXT = 0xFFF3ECE7, MUTED = 0xFFB79E9E, ACCENT = 0xFFFF3B3B, BORDER = 0xFFCE4949;

    private final Screen parent;
    private final ItemListSetting setting;
    private final List<Entry> all = new ArrayList<>();
    private final StringBuilder search = new StringBuilder();
    private boolean searchFocus = true;
    private int scroll;
    private List<Entry> filtered = List.of();
    private String filterKey = "\0";

    private record Entry(String id, String label, ItemStack icon) {}
    private record Hit(int x, int y, int w, int h, String id, boolean selected) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
    private final List<Hit> hits = new ArrayList<>();

    public ItemListEditorScreen(Screen parent, ItemListSetting setting) {
        super(Text.literal("Edit Items"));
        this.parent = parent;
        this.setting = setting;
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            Identifier id = Registries.ITEM.getId(item);
            if (id == null) continue;
            String path = id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
            String label = new ItemStack(item).getName().getString();
            all.add(new Entry(path, label, new ItemStack(item)));
        }
        all.sort(Comparator.comparing(e -> e.label.toLowerCase(Locale.ROOT)));
    }

    @Override public boolean shouldPause() { return false; }

    private int px() { return Math.max(4, (width - PANEL_W) / 2); }
    private int py() { return Math.max(4, (height - PANEL_H) / 2); }

    private List<Entry> filtered() {
        String q = search.toString().trim().toLowerCase(Locale.ROOT);
        if (q.equals(filterKey) && filtered != null) return filtered;
        filterKey = q;
        if (q.isEmpty()) {
            // show selected first, then rest
            List<String> sel = setting.list();
            List<Entry> out = new ArrayList<>();
            for (Entry e : all) if (sel.contains(e.id)) out.add(e);
            for (Entry e : all) if (!sel.contains(e.id)) out.add(e);
            filtered = out;
            return filtered;
        }
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) {
            if (e.id.contains(q) || e.label.toLowerCase(Locale.ROOT).contains(q)) out.add(e);
        }
        filtered = out;
        return filtered;
    }

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        hits.clear();
        c.fill(0, 0, width, height, BG);
        int x = px(), y = py();
        // panel
        c.fill(x + 2, y + 3, x + PANEL_W + 2, y + PANEL_H + 3, 0x50000000);
        c.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL);
        outline(c, x, y, PANEL_W, PANEL_H, BORDER);

        // header
        c.fill(x, y, x + PANEL_W, y + HEAD, HEADER);
        c.fill(x, y + HEAD - 1, x + PANEL_W, y + HEAD, ACCENT);
        c.drawText(textRenderer, Text.literal("Edit Items  (" + setting.list().size() + " selected)"), x + 8, y + 7, TEXT, false);
        // close
        int cx = x + PANEL_W - 18, cy = y + 4;
        hits.add(new Hit(cx, cy, 14, 14, "__close__", false));
        c.drawText(textRenderer, Text.literal("x"), cx + 3, cy + 2, MUTED, false);

        // search
        int sx = x + 10, sy = y + HEAD + 8;
        int sw = PANEL_W - 20;
        c.fill(sx, sy, sx + sw, sy + SEARCH_H, searchFocus ? 0xE81A1E26 : 0xD9121419);
        outline(c, sx, sy, sw, SEARCH_H, searchFocus ? ACCENT : 0xCC9F3A3A);
        hits.add(new Hit(sx, sy, sw, SEARCH_H, "__search__", false));
        String shown = search.toString();
        if (searchFocus && (System.currentTimeMillis() / 400) % 2 == 0) shown = shown + "|";
        String place = shown.isEmpty() && !searchFocus ? "Search items…" : shown;
        c.drawText(textRenderer, Text.literal(fit(place, sw - 12)), sx + 6, sy + 5,
            shown.isEmpty() && !searchFocus ? MUTED : TEXT, false);

        // selected chips strip
        int chipY = sy + SEARCH_H + 6;
        List<String> selected = setting.list();
        int chipX = x + 10;
        int chipMaxR = x + PANEL_W - 10;
        for (String id : selected) {
            String lab = id;
            int cw = textRenderer.getWidth(lab) + 16;
            if (chipX + cw > chipMaxR) break;
            boolean hov = mx >= chipX && mx < chipX + cw && my >= chipY && my < chipY + 12;
            c.fill(chipX, chipY, chipX + cw, chipY + 12, hov ? ACCENT : 0x44FF3B3B);
            c.drawText(textRenderer, Text.literal(lab), chipX + 4, chipY + 2, TEXT, false);
            hits.add(new Hit(chipX, chipY, cw, 12, "__rm__:" + id, true));
            chipX += cw + 4;
        }

        // list
        int listY = chipY + 16;
        int listH = y + PANEL_H - listY - 10;
        int listX = x + 10, listW = PANEL_W - 20;
        c.fill(listX, listY, listX + listW, listY + listH, 0xAA09090B);
        outline(c, listX, listY, listW, listH, 0xCC9F3A3A);

        List<Entry> rows = filtered();
        int content = rows.size() * ROW_H;
        int maxScroll = Math.max(0, content - listH + 4);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        c.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
        try {
            if (rows.isEmpty()) {
                c.drawText(textRenderer, Text.literal("No items matched."), listX + 8, listY + 8, MUTED, false);
            } else {
                int first = scroll / ROW_H;
                int last = Math.min(rows.size() - 1, (scroll + listH) / ROW_H + 1);
                for (int i = first; i <= last; i++) {
                    Entry e = rows.get(i);
                    int ry = listY + 2 - scroll + i * ROW_H;
                    if (ry + ROW_H < listY || ry > listY + listH) continue;
                    boolean on = selected.contains(e.id);
                    boolean hov = mx >= listX && mx < listX + listW && my >= ry && my < ry + ROW_H - 1;
                    int bg = on ? ROW_ON : (hov ? ROW_HOV : ROW);
                    c.fill(listX + 2, ry, listX + listW - 2, ry + ROW_H - 1, bg);
                    if (on) c.fill(listX + 2, ry, listX + 4, ry + ROW_H - 1, ACCENT);
                    c.drawItem(e.icon, listX + 6, ry + (ROW_H - ICON) / 2 - 1);
                    c.drawText(textRenderer, Text.literal(fit(e.label, listW - 80)), listX + 28, ry + 3, on ? ACCENT : TEXT, false);
                    c.drawText(textRenderer, Text.literal(fit(e.id, listW - 80)), listX + 28, ry + 13, MUTED, false);
                    String mark = on ? "ON" : "+";
                    c.drawText(textRenderer, Text.literal(mark), listX + listW - 22, ry + 8, on ? ACCENT : MUTED, false);
                    hits.add(new Hit(listX + 2, ry, listW - 4, ROW_H - 1, e.id, on));
                }
            }
        } finally {
            c.disableScissor();
        }

        // scrollbar
        if (content > listH) {
            int th = Math.max(12, (int) (listH * (listH / (double) content)));
            int ty = listY + (int) ((listH - th) * (scroll / (double) Math.max(1, maxScroll)));
            c.fill(listX + listW - 5, listY, listX + listW - 1, listY + listH, 0x2EFFFFFF);
            c.fill(listX + listW - 5, ty, listX + listW - 1, ty + th, 0xFFC9CDD4);
        }

        // hint
        c.drawText(textRenderer, Text.literal("Click = toggle  ·  Esc = back  ·  chips remove"), x + 10, y + PANEL_H - 12, MUTED, false);
        super.render(c, mx, my, delta);
    }

    private String fit(String s, int maxW) {
        if (s == null) return "";
        if (textRenderer.getWidth(s) <= maxW) return s;
        int end = s.length();
        while (end > 0 && textRenderer.getWidth(s.substring(0, end) + "…") > maxW) end--;
        return s.substring(0, end) + "…";
    }

    private static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col);
        c.fill(x + w - 1, y, x + w, y + h, col);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (click.button() != 0) return true;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(mx, my)) continue;
            if ("__close__".equals(h.id)) { close(); return true; }
            if ("__search__".equals(h.id)) { searchFocus = true; return true; }
            if (h.id.startsWith("__rm__:")) {
                setting.removeItem(h.id.substring(6));
                filterKey = "\0";
                return true;
            }
            // toggle item
            if (setting.list().contains(h.id)) setting.removeItem(h.id);
            else setting.addItem(h.id);
            filterKey = "\0";
            return true;
        }
        searchFocus = false;
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, scroll - (int) Math.round(v * ROW_H));
        return true;
    }

    @Override public boolean charTyped(CharInput input) {
        if (!searchFocus || !input.isValidChar()) return super.charTyped(input);
        String s = input.asString();
        if (s != null && !s.isEmpty() && search.length() < 48) {
            search.append(s);
            filterKey = "\0";
            scroll = 0;
        }
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (k == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (searchFocus && k == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) {
                search.deleteCharAt(search.length() - 1);
                filterKey = "\0";
                scroll = 0;
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override public void close() {
        if (client != null) client.setScreen(parent);
    }
}
