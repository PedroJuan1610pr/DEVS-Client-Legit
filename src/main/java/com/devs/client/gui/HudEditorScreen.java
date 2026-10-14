package com.devs.client.gui;

import com.devs.client.DevsClient;
import com.devs.client.hud.HudLayout;
import com.devs.client.hud.HudLayoutManager;
import com.devs.client.module.Module;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor visual del HUD: arrastra los paneles para moverlos y usa la rueda para escalar.
 * Puede mostrar todos los elementos o solo los de módulos activos.
 */
public final class HudEditorScreen extends Screen {
    private static final int ACCENT = 0xFF4DD2FF;
    private static final int BOX = 0x554DD2FF;
    private static final int BOX_SEL = 0x884DD2FF;
    private static final int BOX_DIM = 0x332A3A4A;
    private static final int BORDER = 0xFF4DD2FF;
    private static final int BORDER_SEL = 0xFFFFFFFF;
    private static final int BORDER_DIM = 0xFF5A6A7A;
    private static final int TEXT = 0xFFF3ECE7;
    private static final int MUTED = 0xFFB79E9E;
    private static final int BAR = 0xE00B0C10;

    private HudLayout drag;
    private float dragOffX, dragOffY;
    private HudLayout selected;
    /** true = todos los paneles posibles; false = solo módulos activos (+ scoreboard). */
    private boolean showAll = false;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override public boolean shouldPause() { return false; }

    private List<HudLayout> visible() {
        List<HudLayout> out = new ArrayList<>();
        for (HudLayout l : HudLayoutManager.all()) {
            if (showAll || isActive(l)) out.add(l);
        }
        return out;
    }

    private boolean isActive(HudLayout l) {
        if (l.scoreboard) return true;
        Module m = moduleForLayout(l);
        return m != null && m.enabled();
    }

    private Module moduleForLayout(HudLayout l) {
        String name = moduleNameForLayout(l.id);
        if (name == null) return null;
        for (Module m : DevsClient.MODULES.all()) {
            if (m.name().equals(name)) return m;
        }
        return null;
    }

    private static String moduleNameForLayout(String id) {
        return switch (id) {
            case HudLayoutManager.HUD_INFO -> "HUD Info";
            case HudLayoutManager.POTION_HUD -> "Potion HUD";
            case HudLayoutManager.ARMOR_HUD -> "Armor HUD";
            case HudLayoutManager.HIT_LOG -> "Hit Log";
            case HudLayoutManager.CUSTOM_DISPLAY -> "Custom Display";
            case HudLayoutManager.MODULE_DISPLAY -> "Module Display";
            case HudLayoutManager.CONSUMABLE_SELF, HudLayoutManager.CONSUMABLE_ENEMY -> "Consumable Counter";
            case HudLayoutManager.TARGET_HUD -> "Target HUD";
            case HudLayoutManager.NOTIFICATIONS -> "Notifications";
            case HudLayoutManager.NETWORK_HUD -> "Network HUD";
            case HudLayoutManager.BLOCK_COUNTER -> "Block Counter";
            case HudLayoutManager.WAYPOINT_HUD -> "Waypoint HUD";
            case HudLayoutManager.SESSION_STATS -> "Session Stats";
            case HudLayoutManager.SPEEDOMETER -> "Speedometer";
            case HudLayoutManager.REACH_DISPLAY -> "Reach Display";
            case HudLayoutManager.COMBO_COUNTER -> "Combo Counter";
            case HudLayoutManager.WORLD_CLOCK -> "World Clock";
            case HudLayoutManager.ELYTRA_HUD -> "Elytra HUD";
            case HudLayoutManager.LIGHT_LEVEL -> "Light Level";
            case HudLayoutManager.TOTEM_INDICATOR -> "Totem Indicator";
            case HudLayoutManager.HOTBAR_WARNING -> "Hotbar Warning";
            case HudLayoutManager.KEYBIND_LIST -> "Keybind List";
            case HudLayoutManager.SLIME_CHUNKS -> "Slime Chunks";
            case HudLayoutManager.PING_GRAPH -> "Ping Graph";
            case HudLayoutManager.IMAGE_DISPLAY -> "Image Display";
            default -> null;
        };
    }

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        HudLayoutManager.refreshScoreboardBounds();
        ensureFallbackBounds();

        c.fill(0, 0, width, 28, BAR);
        c.fill(0, 28, width, 29, ACCENT);
        c.drawTextWithShadow(textRenderer, "HUD Editor", 8, 6, TEXT);
        c.drawTextWithShadow(textRenderer,
            "Drag = move  |  Scroll = scale  |  R = reset  |  Shift+R = reset all  |  Esc = save",
            8, 16, MUTED);

        // Filter toggle + actions
        String filterLabel = showAll ? "All modules" : "Active only";
        drawBtn(c, width - 280, 4, 100, 20, filterLabel, mx, my);
        drawBtn(c, width - 170, 4, 78, 20, "Reset All", mx, my);
        drawBtn(c, width - 82, 4, 74, 20, "Done", mx, my);

        List<HudLayout> list = visible();
        HudLayout hover = hit(mx, my, list);

        for (HudLayout l : list) {
            boolean active = isActive(l);
            boolean on = l == selected || l == hover;
            int sx = l.screenX(), sy = l.screenY(), sw = Math.max(48, l.screenW()), sh = Math.max(28, l.screenH());

            int box = on ? BOX_SEL : (active ? BOX : BOX_DIM);
            int border = on ? BORDER_SEL : (active ? BORDER : BORDER_DIM);
            c.fill(sx, sy, sx + sw, sy + sh, box);
            c.fill(sx, sy, sx + sw, sy + 1, border);
            c.fill(sx, sy + sh - 1, sx + sw, sy + sh, border);
            c.fill(sx, sy, sx + 1, sy + sh, border);
            c.fill(sx + sw - 1, sy, sx + sw, sy + sh, border);

            String status = active ? "" : " (off)";
            String label = l.name + String.format("  %.0f%%%s", l.scale * 100, status);
            int tw = textRenderer.getWidth(label);
            int lx = sx + 4, ly = Math.max(30, sy - 11);
            if (lx + tw > width - 4) lx = Math.max(4, width - 4 - tw);
            c.drawTextWithShadow(textRenderer, label, lx, ly, on ? ACCENT : (active ? TEXT : MUTED));

            int hs = 6;
            c.fill(sx + sw - hs, sy + sh - hs, sx + sw, sy + sh, border);
        }

        String modeHint = showAll
            ? "Showing ALL HUD slots (" + list.size() + "). Grey = module off."
            : "Showing ACTIVE modules only (" + list.size() + "). Click filter to see all.";
        if (selected != null) {
            String info = String.format("%s  x=%.3f  y=%.3f  scale=%.2f", selected.name, selected.x, selected.y, selected.scale);
            c.drawTextWithShadow(textRenderer, info, 8, height - 14, MUTED);
            c.drawTextWithShadow(textRenderer, modeHint, 8, height - 24, MUTED);
        } else {
            c.drawTextWithShadow(textRenderer, modeHint, 8, height - 14, MUTED);
        }

        super.render(c, mx, my, delta);
    }

    private void ensureFallbackBounds() {
        List<HudLayout> list = visible();
        int col = 0, row = 0;
        int cols = Math.max(3, width / 160);
        int cellW = 120, cellH = 44;
        int startY = 40;

        for (HudLayout l : list) {
            boolean needsPlaceholder = l.lastW <= 0 || l.lastH <= 0;
            if (needsPlaceholder) {
                l.lastW = cellW;
                l.lastH = cellH;
                // Grid so "All" mode is readable and not stacked
                if (showAll) {
                    l.lastPx = 12 + col * (cellW + 16);
                    l.lastPy = startY + row * (cellH + 22);
                    col++;
                    if (col >= cols) { col = 0; row++; }
                } else {
                    if (l.scoreboard) {
                        l.lastPx = l.x * width - l.lastW * l.scale;
                        l.lastPy = l.y * height;
                    } else {
                        l.lastPx = l.x * width;
                        l.lastPy = l.y * height;
                    }
                }
            }
            // Minimum size so boxes are always grabable
            if (l.lastW < 48) l.lastW = 48;
            if (l.lastH < 28) l.lastH = 28;
        }
    }

    private void drawBtn(DrawContext c, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        c.fill(x, y, x + w, y + h, hov ? 0xFF242832 : 0xFF181A20);
        c.fill(x, y, x + w, y + 1, ACCENT);
        int tw = textRenderer.getWidth(label);
        c.drawTextWithShadow(textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, TEXT);
    }

    private HudLayout hit(double mx, double my, List<HudLayout> list) {
        HudLayout best = null;
        for (HudLayout l : list) {
            int sx = l.screenX(), sy = l.screenY();
            int sw = Math.max(48, l.screenW()), sh = Math.max(28, l.screenH());
            if (mx >= sx && mx < sx + sw && my >= sy && my < sy + sh) best = l;
        }
        return best;
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int b = click.button();
        if (b != 0) return super.mouseClicked(click, doubled);

        if (my >= 4 && my <= 24) {
            if (mx >= width - 280 && mx < width - 180) {
                showAll = !showAll;
                selected = null;
                return true;
            }
            if (mx >= width - 170 && mx < width - 92) {
                HudLayoutManager.resetAll();
                return true;
            }
            if (mx >= width - 82 && mx < width - 8) {
                close();
                return true;
            }
        }

        HudLayout l = hit(mx, my, visible());
        if (l != null) {
            selected = l;
            drag = l;
            if (l.scoreboard) {
                dragOffX = (float) mx - (l.screenX() + Math.max(48, l.screenW()));
                dragOffY = (float) my - l.screenY();
            } else {
                dragOffX = (float) mx - l.screenX();
                dragOffY = (float) my - l.screenY();
            }
            return true;
        }
        selected = null;
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (drag == null) return super.mouseDragged(click, offsetX, offsetY);
        double mx = click.x(), my = click.y();
        int sw = width, sh = height;
        if (drag.scoreboard) {
            float right = (float) mx - dragOffX;
            float top = (float) my - dragOffY;
            HudLayoutManager.moveFromScreen(drag, right, top, sw, sh);
            drag.lastPx = right - drag.lastW * drag.scale;
            drag.lastPy = top;
        } else {
            float left = (float) mx - dragOffX;
            float top = (float) my - dragOffY;
            HudLayoutManager.moveFromScreen(drag, left, top, sw, sh);
            drag.lastPx = left;
            drag.lastPy = top;
        }
        return true;
    }

    @Override public boolean mouseReleased(Click click) {
        if (drag != null) {
            drag = null;
            HudLayoutManager.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        HudLayout l = selected != null ? selected : hit(mx, my, visible());
        if (l == null) return super.mouseScrolled(mx, my, horizontal, vertical);
        float next = l.scale + (float) (vertical * 0.08);
        l.scale = Math.max(0.4f, Math.min(3f, next));
        if (l.scoreboard) {
            float right = l.x * width;
            float top = l.y * height;
            l.lastPx = right - l.lastW * l.scale;
            l.lastPy = top;
        }
        HudLayoutManager.save();
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (k == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (k == GLFW.GLFW_KEY_R) {
            if (input.hasShift()) HudLayoutManager.resetAll();
            else if (selected != null) { selected.reset(); HudLayoutManager.save(); }
            else HudLayoutManager.resetAll();
            return true;
        }
        if (k == GLFW.GLFW_KEY_TAB) {
            showAll = !showAll;
            selected = null;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override public void close() {
        HudLayoutManager.save();
        super.close();
    }
}
