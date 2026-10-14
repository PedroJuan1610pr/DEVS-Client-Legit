package com.devs.client.gui;

import com.devs.client.DevsClient;
import com.devs.client.module.GuiThemeModule;
import com.devs.client.module.Module;
import com.devs.client.module.ModuleManager;
import com.devs.client.setting.*;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ClickGUI de DEVS con la interfaz del AUTISM Client (solo la GUI, sin sus modulos).
 *
 *  - Una ventana plana por categoria: arrastrable, plegable, con scroll propio.
 *  - Clic izq. en modulo: activar/desactivar   |   Clic der. en modulo: abrir sus ajustes
 *  - Clic en cabecera (sin arrastrar) o clic der.: plegar/desplegar la ventana
 *  - Ventana de ajustes centrada: Enabled, Reset, descripcion y lista de ajustes con scroll
 *  - Esc: cierra popup -> ajustes -> GUI
 */
public final class ClickGuiScreen extends Screen {
    // Dynamic colors (filled each frame from GuiTheme = Autism palette)
    private int SCRIM, WINDOW, WINDOW_STRONG, HEADER, HEADER_HOVER, ROW, ROW_HOVER;
    private int FIELD, FIELD_FOCUSED, TEXT, MUTED, DISABLED, HAIRLINE, OFF_TRACK, KNOB, BTN_FILL, BTN_ACTIVE;
    private int BORDER, BORDER_SOFT, BUTTON_BORDER, ACCENT, ACCENT_SOFT;

    private void syncTheme() {
        for (Module m : modules.all()) {
            if (m instanceof GuiThemeModule g) { g.apply(); break; }
        }
        SCRIM = GuiTheme.SCRIM;
        WINDOW = GuiTheme.WINDOW;
        WINDOW_STRONG = GuiTheme.WINDOW_STRONG;
        HEADER = GuiTheme.HEADER;
        HEADER_HOVER = GuiTheme.HEADER_HOVER;
        ROW = GuiTheme.ROW;
        ROW_HOVER = GuiTheme.ROW_HOVER;
        FIELD = GuiTheme.FIELD;
        FIELD_FOCUSED = GuiTheme.FIELD_FOCUSED;
        TEXT = GuiTheme.TEXT;
        MUTED = GuiTheme.MUTED;
        DISABLED = GuiTheme.DISABLED;
        HAIRLINE = GuiTheme.HAIRLINE;
        OFF_TRACK = GuiTheme.OFF_TRACK;
        KNOB = GuiTheme.KNOB;
        BTN_FILL = GuiTheme.BTN_FILL;
        BTN_ACTIVE = GuiTheme.BTN_ACTIVE;
        ACCENT = GuiTheme.ACCENT;
        ACCENT_SOFT = GuiTheme.ACCENT_SOFT;
        BORDER = GuiTheme.BORDER;
        BORDER_SOFT = GuiTheme.BORDER_SOFT;
        BUTTON_BORDER = GuiTheme.BUTTON_BORDER;
    }

    // ---------- medidas ----------
    private static final int HEAD = 15, ROW_H = 15, SET_H = 22, SET_GAP = 2, BTN_H = 15, SB = 6;
    private static final int MAX_ROWS = 9, MIN_W = 88, MAX_W = 150, WIN_GAP = 6;
    private static final int SETTINGS_W = 360, SLIDER_CONTROL_W = 170, VALUE_W = 44, SLIDER_MIN_W = 36;
    private static final int INFO_W = 112, INFO_BODY_H = 72;
    private static final int PK_PAD = 6, PK_SV_W = 96, PK_SV_H = 64, PK_HUE_H = 8, PK_SW = 10;
    private static final int PK_W = PK_PAD * 2 + PK_SV_W, PK_H = PK_PAD + PK_SV_H + 5 + PK_HUE_H + 5 + PK_SW + PK_PAD;

    private static final Identifier LOGO = Identifier.of("devs", "textures/gui/logo.png");
    private static final int LOGO_TEX = 48, LOGO_SIZE = 24;

    private static final String[] CATS = {"Combat", "Movement", "Player", "Render", "World", "Misc"};
    private static final int[] PALETTE = {
        0xFFFF4D6D, 0xFFFFB347, 0xFFB56CFF, 0xFF9CA3AF, 0xFF4DD2FF, 0xFFB6F36A, 0xFFFFFFFF, 0xFFFF6BD6
    };

    // ---------- estado persistente entre aperturas ----------
    /** category == null -> ventana de informacion (logo DEVS). */
    private static final class Win {
        final String cat;
        int x, y, scroll;
        boolean collapsed;
        Win(String cat) { this.cat = cat; }
    }
    /** Orden de dibujado: el ultimo queda encima. */
    private static final List<Win> WINS = new ArrayList<>();
    /** Si el usuario arrastra ventanas, no reordenar al reabrir. Right-Shift+R o botón Reset Layout resetea. */
    private static boolean customLayout = false;

    // ---------- geometria / hits ----------
    private record R(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
        R inset(int n) { return new R(x + n, y + n, Math.max(0, w - n * 2), Math.max(0, h - n * 2)); }
        R intersect(R o) {
            int nx = Math.max(x, o.x), ny = Math.max(y, o.y);
            int nr = Math.min(right(), o.right()), nb = Math.min(bottom(), o.bottom());
            return new R(nx, ny, Math.max(0, nr - nx), Math.max(0, nb - ny));
        }
    }

    private enum HitType {
        WIN_BODY, WIN_HEADER, WIN_SCROLLBAR, MODULE, MODULE_BIND, SEARCH_FIELD,
        SET_BODY, SET_CLOSE, SET_ENABLED, SET_BIND, SET_RESET, SET_SEARCH, SET_BOOL, SET_SLIDER, SET_DROPDOWN, SET_COLOR, SET_TEXT, SET_ITEMS, SET_SCROLLBAR
    }
    private record Hit(HitType type, R r, Win win, Module module, Setting<?> setting) {}

    private final ModuleManager modules = DevsClient.MODULES;
    private final Map<String, List<Module>> byCat = new HashMap<>();
    private final List<String> order = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Float> anims = new HashMap<>();
    private final Map<String, Long> animFrameStamp = new HashMap<>();
    private long animFrame = 0;
    private final Map<Setting<?>, R> sliderRects = new IdentityHashMap<>();

    private Module selected;                 // modulo cuya ventana de ajustes esta abierta
    private int setScroll, setContent, setView;
    private String tooltip;

    private Win dragWin;
    private int dragOffX, dragOffY, dragStartX, dragStartY;
    private boolean dragMoved;
    private Setting<?> sliderSetting;

    private boolean sbActive;
    private Win sbWin;
    private R sbTrack;
    private int sbContent, sbView, sbGrab;

    private ModeSetting dropdown;
    private R dropAnchor;

    private ColorSetting picker;
    private R pickAnchor;
    private float pHue, pSat, pVal;
    private int pDrag;                       // 0 nada, 1 cuadro S/V, 2 barra de tono

    // Item list picker (estilo Autism registry list)
    private ItemListSetting itemListEdit;
    private R itemListAnchor;
    private final StringBuilder itemSearchBuf = new StringBuilder();
    private boolean itemSearchFocused;
    private int itemScroll;
    private List<Item> itemFilterCache = List.of();
    private String itemFilterQuery = "\0";

    private long lastNs = 0, openNs = 0;
    private float dt = 0.016f;

    private StringSetting textEdit;
    private final StringBuilder textBuf = new StringBuilder();

    private Module bindingModule;            // capturing keybind (row or settings)
    private String searchQuery = "";
    private boolean searchFocused;
    private final StringBuilder searchBuf = new StringBuilder();
    private String settingsSearch = "";
    private boolean settingsSearchFocused;
    private final StringBuilder settingsSearchBuf = new StringBuilder();


    public ClickGuiScreen() {
        super(Text.literal("DEVS"));
        rebuildCategories();
    }

    /** Agrupa módulos por categoría (orden fijos CATS) y ordena A→Z dentro de cada una. */
    private void rebuildCategories() {
        byCat.clear();
        order.clear();
        for (Module m : modules.all()) {
            byCat.computeIfAbsent(m.category(), k -> new ArrayList<>()).add(m);
        }
        for (List<Module> list : byCat.values()) {
            list.sort(Comparator.comparing(a -> a.name().toLowerCase(Locale.ROOT)));
        }
        for (String c : CATS) if (byCat.containsKey(c) && !byCat.get(c).isEmpty()) order.add(c);
        for (String c : byCat.keySet()) if (!order.contains(c) && !byCat.get(c).isEmpty()) order.add(c);
    }

    @Override protected void init() {
        openNs = System.nanoTime();
        rebuildCategories();
        syncWindows();
        if (!customLayout || isLayoutBroken()) {
            layoutWindows();
            customLayout = false;
        } else {
            clampAllWindows();
        }
    }

    /** Crea/elimina ventanas para que coincidan con las categorías actuales + Info + Search. */
    private void syncWindows() {
        // mapa cat -> win existente
        Map<String, Win> have = new HashMap<>();
        for (Win w : WINS) {
            String key = w.cat == null ? "\0INFO" : w.cat;
            have.put(key, w);
        }
        List<Win> next = new ArrayList<>();
        // Info
        next.add(have.getOrDefault("\0INFO", new Win(null)));
        // Search
        next.add(have.getOrDefault("Search", new Win("Search")));
        // Categories in order
        for (String cat : order) {
            if (mods(cat).isEmpty()) continue;
            next.add(have.getOrDefault(cat, new Win(cat)));
        }
        WINS.clear();
        WINS.addAll(next);
    }

    /** True si hay solapes graves o todas las ventanas en el mismo sitio. */
    private boolean isLayoutBroken() {
        if (WINS.size() < 2) return true;
        int same = 0;
        Win a = WINS.get(0);
        for (Win w : WINS) {
            if (w.x == a.x && w.y == a.y) same++;
            if (w.x == 0 && w.y == 0) return true;
        }
        if (same >= Math.max(3, WINS.size() - 1)) return true;
        // fuera de pantalla
        int off = 0;
        for (Win w : WINS) {
            if (w.x > width - 20 || w.y > height - 20 || w.x < -50 || w.y < -50) off++;
        }
        return off >= 2;
    }

    /**
     * Layout estilo Autism: columnas por categoría, de izq→der, wrap a la siguiente fila
     * con gap uniforme. No se apilan.
     */
    private void layoutWindows() {
        final int margin = 8;
        final int gapX = Math.max(WIN_GAP, 8);
        final int gapY = 10;
        int x = margin, y = margin, rowH = 0;
        int screenW = Math.max(320, width);

        for (Win w : WINS) {
            w.scroll = 0;
            int ww = winW(w);
            int body = animatedBodyH(w);
            int wh = HEAD + body + (body > 0 ? 1 : 0);

            // wrap a nueva fila si no cabe
            if (x > margin && x + ww > screenW - margin) {
                x = margin;
                y += rowH + gapY;
                rowH = 0;
            }
            w.x = x;
            w.y = y;
            rowH = Math.max(rowH, wh);
            x += ww + gapX;
        }
        // si se salió por abajo, segunda pasada más compacta (columnas fijas)
        int maxBottom = 0;
        for (Win w : WINS) {
            int body = animatedBodyH(w);
            maxBottom = Math.max(maxBottom, w.y + HEAD + body);
        }
        if (maxBottom > height - 8 && height > 200) {
            layoutWindowsGrid();
        }
    }

    /** Fallback: grid de columnas de ancho fijo (muy ordenado con muchas categorías). */
    private void layoutWindowsGrid() {
        final int margin = 6;
        final int gapX = 6, gapY = 8;
        // calcular anchos
        int n = WINS.size();
        if (n == 0) return;
        // ~ columnas que quepan con MIN_W
        int colW = Math.min(MAX_W, Math.max(MIN_W, (width - margin * 2 - gapX * 4) / Math.min(n, 6)));
        int cols = Math.max(1, (width - margin) / (colW + gapX));
        int i = 0;
        int[] colHeights = new int[cols];
        for (Win w : WINS) {
            w.scroll = 0;
            int body = animatedBodyH(w);
            int wh = HEAD + body + (body > 0 ? 1 : 0);
            // columna con menor altura
            int best = 0;
            for (int c = 1; c < cols; c++) if (colHeights[c] < colHeights[best]) best = c;
            w.x = margin + best * (colW + gapX);
            w.y = margin + colHeights[best];
            colHeights[best] += wh + gapY;
            i++;
        }
    }

    private void clampAllWindows() {
        for (Win w : WINS) {
            int body = animatedBodyH(w);
            int h = HEAD + body + (body > 0 ? 1 : 0);
            int ww = winW(w);
            w.x = clamp(w.x, 4, Math.max(4, width - 4 - ww));
            w.y = clamp(w.y, 4, Math.max(4, height - 4 - h));
        }
    }

    @Override public boolean shouldPause() { return false; }

    // =====================================================================
    //  Geometria de ventanas
    // =====================================================================
    private List<Module> mods(String cat) { return byCat.getOrDefault(cat, List.of()); }

    private int maxBodyH() { return Math.max(42, Math.min(height - 8 - HEAD, MAX_ROWS * ROW_H)); }

    private int winContentH(Win w) {
        if (w.cat == null) return INFO_BODY_H;
        if ("Search".equals(w.cat)) return BTN_H + 4 + Math.max(1, searchResults().size()) * ROW_H;
        return mods(w.cat).size() * ROW_H;
    }

    /** 0 = fully collapsed, 1 = fully open (smooth ease-in-out). */
    private float expandT(Win w) {
        String key = "exp:" + (w.cat == null ? "_info" : w.cat);
        float t = anim(key, w.collapsed ? 0f : 1f);
        // smoothstep
        return t * t * (3f - 2f * t);
    }

    /** Animated body height (pixels) for a category window. */
    private int animatedBodyH(Win w) {
        int content = winContentH(w);
        int full = w.cat == null ? content : Math.min(content, maxBodyH());
        return Math.max(0, Math.round(full * expandT(w)));
    }

    private List<Module> searchResults() {
        String q = (searchFocused ? searchBuf.toString() : searchQuery).trim().toLowerCase();
        List<Module> out = new ArrayList<>();
        for (Module m : modules.all()) {
            if (q.isEmpty() || m.name().toLowerCase().contains(q) || m.category().toLowerCase().contains(q)
                || (m.description() != null && m.description().toLowerCase().contains(q))) {
                out.add(m);
            }
        }
        // cap for scroll
        return out;
    }

    private List<Setting<?>> filteredSettings(Module m) {
        String q = (settingsSearchFocused ? settingsSearchBuf.toString() : settingsSearch).trim().toLowerCase();
        if (q.isEmpty()) return m.settings();
        List<Setting<?>> out = new ArrayList<>();
        for (Setting<?> s : m.settings()) {
            if (s.name().toLowerCase().contains(q)) out.add(s);
        }
        return out;
    }

    private int winW(Win w) {
        if (w.cat == null) return INFO_W;
        if ("Search".equals(w.cat)) return Math.min(150, Math.max(MIN_W, Math.max(1, width - 8)));
        int maxName = 0;
        for (Module m : mods(w.cat)) maxName = Math.max(maxName, textRenderer.getWidth(m.name()));
        boolean sb = winContentH(w) > maxBodyH();
        // leave room for keybind button (34) like Autism CompactKeybindButton
        int rowNeed = 5 + maxName + 9 + 8 + 34 + (sb ? SB : 0) + 2;
        int headNeed = textRenderer.getWidth(w.cat) + 26;
        int ww = Math.max(MIN_W, Math.max(rowNeed, headNeed));
        return Math.min(ww, Math.min(MAX_W + 20, Math.max(1, width - 8)));
    }

    private R winBounds(Win w) {
        int cw = winW(w);
        int body = animatedBodyH(w);
        int h = HEAD + body + (body > 0 ? 1 : 0);
        int x = clamp(w.x, 4, Math.max(4, width - 4 - cw));
        int y = clamp(w.y, 4, Math.max(4, height - 4 - h));
        return new R(x, y, cw, h);
    }

    private boolean visible(Win w) { return w.cat == null || "Search".equals(w.cat) || !mods(w.cat).isEmpty(); }

    private Win winAt(double mx, double my) {
        for (int i = WINS.size() - 1; i >= 0; i--) {
            Win w = WINS.get(i);
            if (visible(w) && winBounds(w).contains(mx, my)) return w;
        }
        return null;
    }

    private void bringToFront(Win w) { WINS.remove(w); WINS.add(w); }

    // =====================================================================
    //  Render
    // =====================================================================
    @Override public void render(DrawContext c, int mx, int my, float delta) {
        syncTheme();
        long now = System.nanoTime();
        dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;
        animFrame++; // advance animations at most once per frame
        float fade = Math.min(1f, (now - openNs) / 220_000_000f); // slightly longer open fade

        hits.clear();
        tooltip = null;
        c.fill(0, 0, width, height, withAlpha(SCRIM, fade));

        boolean popup = dropdown != null || picker != null || itemListEdit != null || bindingModule != null;
        int bx = popup ? -10000 : mx, by = popup ? -10000 : my;

        if (selected == null) {
            Win top = winAt(bx, by);
            for (Win w : WINS) renderWindow(c, w, bx, by, w == top);
        } else {
            renderSettings(c, bx, by);
        }
        if (dropdown != null) renderDropdown(c, mx, my);
        if (picker != null) renderPicker(c);
        if (itemListEdit != null) renderItemPicker(c, mx, my);
        if (tooltip != null && !popup) drawTooltip(c, tooltip, mx, my);
        super.render(c, mx, my, delta);
    }

    // ---------- ventanas de categoria ----------
    private void renderWindow(DrawContext c, Win w, int mx, int my, boolean hoverWin) {
        if (!visible(w)) return;
        R b = winBounds(w);
        R head = new R(b.x(), b.y(), b.w(), HEAD);
        frame(c, b, WINDOW, BORDER_SOFT);
        hit(HitType.WIN_BODY, b, w, null, null);

        rect(c, head, hoverWin && head.contains(mx, my) ? HEADER_HOVER : HEADER);
        if (animatedBodyH(w) > 0) hEdge(c, b.x(), b.y() + HEAD - 1, b.w(), ACCENT);
        chevron(c, b.x() + 8, b.y() + HEAD / 2, expandT(w) < 0.5f ? 0 : 1, TEXT);
        String title = w.cat == null ? "DEVS" : w.cat;
        str(c, fit(title, b.w() - 22), b.x() + 17, b.y() + 3, TEXT);
        hit(HitType.WIN_HEADER, head, w, null, null);
        int animBody = animatedBodyH(w);
        if (animBody <= 0) return;

        int bodyH = b.h() - HEAD - 1;
        if (w.cat == null) { renderInfoBody(c, new R(b.x() + 1, b.y() + HEAD, b.w() - 2, bodyH)); return; }
        if ("Search".equals(w.cat)) { renderSearchBody(c, w, new R(b.x() + 1, b.y() + HEAD, b.w() - 2, bodyH), mx, my, hoverWin); return; }

        List<Module> list = mods(w.cat);
        int content = list.size() * ROW_H;
        boolean needSb = content > bodyH;
        R vp = new R(b.x() + 1, b.y() + HEAD, b.w() - 2 - (needSb ? SB : 0), bodyH);
        w.scroll = clamp(w.scroll, 0, Math.max(0, content - bodyH));

        c.enableScissor(vp.x(), vp.y(), vp.right(), vp.bottom());
        try {
            int first = Math.max(0, w.scroll / ROW_H);
            int last = Math.min(list.size() - 1, (w.scroll + bodyH - 1) / ROW_H);
            for (int i = first; i <= last; i++) {
                R row = new R(vp.x(), vp.y() - w.scroll + i * ROW_H, vp.w(), ROW_H);
                R clip = row.intersect(vp);
                boolean hov = hoverWin && clip.contains(mx, my);
                renderModuleRow(c, list, i, row, hov);
            }
        } finally {
            c.disableScissor();
        }
        if (needSb) {
            R track = new R(b.right() - SB - 1, vp.y(), SB, bodyH);
            boolean hov = hoverWin && track.contains(mx, my);
            drawScrollbar(c, track, content, bodyH, w.scroll, hov, sbActive && sbWin == w);
            hit(HitType.WIN_SCROLLBAR, track, w, null, null);
        }
    }

    private void renderSearchBody(DrawContext c, Win w, R body, int mx, int my, boolean hoverWin) {
        // search field
        R field = new R(body.x() + 2, body.y() + 2, body.w() - 4, BTN_H);
        boolean fHov = field.contains(mx, my);
        frame(c, field, searchFocused ? FIELD_FOCUSED : (fHov ? ROW_HOVER : FIELD), searchFocused ? ACCENT : BORDER_SOFT);
        String shown = searchFocused ? searchBuf.toString() : searchQuery;
        if (searchFocused && (System.currentTimeMillis() / 400) % 2 == 0) shown = shown + "|";
        str(c, fit(shown.isEmpty() && !searchFocused ? "Search modules..." : shown, field.w() - 10), field.x() + 5, field.y() + 3, searchFocused || !shown.isEmpty() ? TEXT : MUTED);
        hit(HitType.SEARCH_FIELD, field, w, null, null);

        List<Module> list = searchResults();
        int listTop = field.bottom() + 2;
        int listH = Math.max(0, body.bottom() - listTop);
        int content = list.size() * ROW_H;
        boolean needSb = content > listH;
        R vp = new R(body.x(), listTop, body.w() - (needSb ? SB : 0), listH);
        w.scroll = clamp(w.scroll, 0, Math.max(0, content - listH));
        c.enableScissor(vp.x(), vp.y(), vp.right(), vp.bottom());
        try {
            int first = Math.max(0, w.scroll / ROW_H);
            int last = Math.min(list.size() - 1, (w.scroll + listH - 1) / ROW_H);
            for (int i = first; i <= last; i++) {
                R row = new R(vp.x(), vp.y() - w.scroll + i * ROW_H, vp.w(), ROW_H);
                R clip = row.intersect(vp);
                boolean hov = hoverWin && clip.contains(mx, my);
                renderModuleRow(c, list, i, row, hov);
            }
        } finally {
            c.disableScissor();
        }
        if (needSb) {
            R track = new R(body.right() - SB, listTop, SB, listH);
            drawScrollbar(c, track, content, listH, w.scroll, track.contains(mx, my), sbActive && sbWin == w);
            hit(HitType.WIN_SCROLLBAR, track, w, null, null);
        }
    }

    private void renderInfoBody(DrawContext c, R body) {
        rect(c, body, ROW);
        c.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, body.x() + 6, body.y() + 6, 0f, 0f,
            LOGO_SIZE, LOGO_SIZE, LOGO_TEX, LOGO_TEX, LOGO_TEX, LOGO_TEX);
        Text name = Text.literal("DEVS").withColor(TEXT).formatted(Formatting.BOLD);
        c.drawText(textRenderer, name, body.x() + 36, body.y() + 8, 0xFFFFFFFF, false);
        str(c, "Client", body.x() + 36, body.y() + 19, MUTED);
        rect(c, new R(body.x() + 4, body.y() + 34, body.w() - 8, 1), HAIRLINE);
        str(c, "LMB  toggle", body.x() + 6, body.y() + 38, MUTED);
        str(c, "RMB  settings", body.x() + 6, body.y() + 48, MUTED);
        str(c, "H    HUD edit", body.x() + 6, body.y() + 58, MUTED);
    }

    private void renderModuleRow(DrawContext c, List<Module> list, int i, R row, boolean hov) {
        Module m = list.get(i);
        float h = anim("h:" + m.name(), hov ? 1f : 0f);
        float on = anim("on:" + m.name(), m.enabled() ? 1f : 0f);
        rect(c, row, lerp(ROW, ROW_HOVER, h));
        if (on > 0.001f) {
            int fw = Math.max(1, Math.round(row.w() * on));
            rect(c, new R(row.x(), row.y(), fw, row.h()), withAlpha(ACCENT, hov ? 0.34f : 0.22f));
            int col = withAlpha(ACCENT, on);
            if (i > 0 && onProgress(list.get(i - 1)) <= 0.001f) hEdge(c, row.x(), row.y(), fw, col);
            if (i + 1 >= list.size() || onProgress(list.get(i + 1)) <= 0.001f) hEdge(c, row.x(), row.bottom() - 1, fw, col);
            vEdge(c, row.x(), row.y(), row.h(), col);
            vEdge(c, row.right() - 1, row.y(), row.h(), col);
            rect(c, new R(row.x(), row.y(), 2, row.h()), col);
        }
        // Autism CompactKeybindButton at row end
        int bindW = 34;
        R bind = new R(row.right() - bindW - 3, row.y() + 2, bindW, Math.max(1, row.h() - 4));
        boolean binding = bindingModule == m;
        int maxW = Math.max(1, bind.x() - row.x() - 9);
        String label = binding ? "Press key" : m.name();
        str(c, fit(label, maxW), row.x() + 5 + Math.round(on * 1f), row.y() + 3, binding ? ACCENT : lerp(MUTED, TEXT, on));
        button(c, bind, binding ? "Key" : (m.keybind() < 0 ? "Bind" : BindUtil.name(m.keybind())), "kb:" + m.name(), hov, binding);
        hit(HitType.MODULE, row, null, m, null);
        hit(HitType.MODULE_BIND, bind, null, m, null);
        if (hov && m.description() != null && !m.description().isBlank() && !binding) tooltip = m.description();
    }

    private float onProgress(Module m) { return anims.getOrDefault("on:" + m.name(), m.enabled() ? 1f : 0f); }

    // ---------- ventana de ajustes ----------
    private void renderSettings(DrawContext c, int mx, int my) {
        Module m = selected;
        List<Setting<?>> sets = filteredSettings(m);
        int w = Math.min(SETTINGS_W, width - 8);
        int content = sets.isEmpty() ? 0 : sets.size() * (SET_H + SET_GAP) - SET_GAP;
        // toolbar (Enabled+Bind+Reset) + search + desc
        int top = HEAD + 5 + BTN_H + 5 + BTN_H + 5 + 9 + 5;
        int bodyH = Math.max(0, Math.min(content, Math.max(42, height - 12 - top - 5)));
        int hFull = top + bodyH + 5;
        // open animation (scale + fade from center)
        float open = anim("setOpen:" + m.name(), 1f);
        float ease = 1f - (1f - open) * (1f - open);
        int h = Math.max(HEAD + 8, Math.round(hFull * (0.85f + 0.15f * ease)));
        int x = Math.max(4, (width - w) / 2), y = Math.max(4, (height - h) / 2);
        R b = new R(x, y, w, h);
        R head = new R(x, y, w, HEAD);

        frame(c, b, WINDOW, BORDER_SOFT);
        hit(HitType.SET_BODY, b, null, m, null);
        rect(c, head, head.contains(mx, my) ? HEADER_HOVER : HEADER);
        hEdge(c, x, y + HEAD - 1, w, ACCENT);
        str(c, fit(m.name(), w - 30), x + 5, y + 3, TEXT);

        R close = new R(b.right() - 16, y + 1, 14, 14);
        boolean closeHov = close.contains(mx, my);
        if (closeHov) disc(c, close.x() + 7f, close.y() + 7f, 6.5f, ACCENT_SOFT);
        drawX(c, close.x() + 3, close.y() + 3, closeHov ? TEXT : MUTED);
        hit(HitType.SET_CLOSE, close, null, m, null);

        // barra Autism: Enabled + Bind + Reset
        int ty = y + HEAD + 5;
        R en = new R(x + 5, ty, 88, BTN_H);
        boolean enHov = en.contains(mx, my);
        str(c, "Enabled", en.x() + 5, en.y() + 3, m.enabled() ? TEXT : MUTED);
        drawToggle(c, new R(en.right() - 28, en.y() + 3, 26, 9), "en:" + m.name(), m.enabled(), enHov);
        hit(HitType.SET_ENABLED, en, null, m, null);

        R bindBtn = new R(en.right() + 4, ty, 88, BTN_H);
        boolean binding = bindingModule == m;
        button(c, bindBtn, binding ? "Press key" : "Bind " + BindUtil.name(m.keybind()), "setbind:" + m.name(), bindBtn.contains(mx, my), binding);
        hit(HitType.SET_BIND, bindBtn, null, m, null);

        R reset = new R(b.right() - 5 - 52, ty, 52, BTN_H);
        button(c, reset, "Reset", "reset", reset.contains(mx, my), false);
        hit(HitType.SET_RESET, reset, null, m, null);

        // settings search (Autism)
        R sfield = new R(x + 5, ty + BTN_H + 5, w - 10, BTN_H);
        frame(c, sfield, settingsSearchFocused ? FIELD_FOCUSED : FIELD, settingsSearchFocused ? ACCENT : BORDER_SOFT);
        String sq = settingsSearchFocused ? settingsSearchBuf.toString() : settingsSearch;
        if (settingsSearchFocused && (System.currentTimeMillis() / 400) % 2 == 0) sq = sq + "|";
        str(c, fit(sq.isEmpty() && !settingsSearchFocused ? "Search settings..." : sq, sfield.w() - 10), sfield.x() + 5, sfield.y() + 3,
            settingsSearchFocused || !sq.isEmpty() ? TEXT : MUTED);
        hit(HitType.SET_SEARCH, sfield, null, m, null);

        String desc = m.description() == null ? "" : m.description();
        str(c, fit(desc, w - 10), x + 5, sfield.bottom() + 3, MUTED);

        // lista de ajustes
        int bodyTop = sfield.bottom() + 5 + 9 + 2;
        R body = new R(x + 5, bodyTop, w - 10, Math.max(1, b.bottom() - bodyTop - 5));
        boolean needSb = content > body.h();
        R vp = needSb ? new R(body.x(), body.y(), body.w() - SB - 2, body.h()) : body;
        setContent = content; setView = body.h();
        setScroll = clamp(setScroll, 0, Math.max(0, content - body.h()));
        if (sets.isEmpty()) str(c, "No settings.", body.x() + 5, body.y() + 3, MUTED);

        c.enableScissor(vp.x(), vp.y(), vp.right(), vp.bottom());
        try {
            for (int i = 0; i < sets.size(); i++) {
                R row = new R(vp.x(), vp.y() - setScroll + i * (SET_H + SET_GAP), vp.w(), SET_H);
                if (row.bottom() <= vp.y() || row.y() >= vp.bottom()) continue;
                renderSettingRow(c, m, sets.get(i), row, vp, mx, my);
            }
        } finally {
            c.disableScissor();
        }
        if (needSb) {
            R track = new R(body.right() - SB, body.y(), SB, body.h());
            drawScrollbar(c, track, content, body.h(), setScroll, track.contains(mx, my), sbActive && sbWin == null);
            hit(HitType.SET_SCROLLBAR, track, null, m, null);
        }
    }

    private void renderSettingRow(DrawContext c, Module m, Setting<?> s, R row, R vp, int mx, int my) {
        R clip = row.intersect(vp);
        boolean hov = clip.contains(mx, my);
        String key = m.name() + ":" + s.name();
        float hf = anim("s:" + key, hov ? 1f : 0f);
        rect(c, row, lerp(ROW, ROW_HOVER, hf));

        boolean wide = s instanceof NumberSetting;
        int controlW = wide ? Math.min(SLIDER_CONTROL_W, Math.max(96, row.w() * 3 / 5))
                            : Math.min(132, Math.max(96, row.w() / 3));
        int labelW = Math.max(40, row.w() - controlW - 8);
        str(c, fit(s.name(), labelW - 5), row.x() + 5, row.y() + (SET_H - 9) / 2, TEXT);
        R ctl = new R(row.right() - controlW - 4, row.y() + Math.max(3, (SET_H - BTN_H) / 2), controlW, BTN_H);

        if (s instanceof BooleanSetting bs) {
            R t = new R(ctl.right() - 34, ctl.y(), 34, ctl.h());
            drawToggle(c, t, "bs:" + key, bs.get(), hov);
            hit(HitType.SET_BOOL, clip, null, m, s);
        } else if (s instanceof NumberSetting n) {
            R sl = new R(ctl.x(), ctl.y(), Math.max(SLIDER_MIN_W, ctl.w() - 4 - VALUE_W), ctl.h());
            R vb = new R(ctl.right() - VALUE_W, ctl.y(), VALUE_W, ctl.h());
            double ratio = (n.get() - n.min()) / Math.max(0.0001, n.max() - n.min());
            boolean held = sliderSetting == s;
            sliderRects.put(s, new R(sl.x() + 3, sl.y(), Math.max(1, sl.w() - 6), sl.h()));
            drawSlider(c, sl, ratio, held || sl.contains(mx, my));
            frame(c, vb, held ? FIELD_FOCUSED : FIELD, held ? ACCENT : BORDER_SOFT);
            str(c, fit(value(s), vb.w() - 8), vb.x() + 4, vb.y() + 3, TEXT);
            hit(HitType.SET_SLIDER, sl.intersect(vp), null, m, s);
        } else if (s instanceof ModeSetting ms) {
            boolean open = dropdown == ms;
            boolean ch = ctl.contains(mx, my);
            frame(c, ctl, open ? FIELD_FOCUSED : (ch ? ROW_HOVER : FIELD), open ? ACCENT : BORDER_SOFT);
            str(c, fit(ms.get(), ctl.w() - 18), ctl.x() + 5, ctl.y() + 3, TEXT);
            chevron(c, ctl.right() - 8, ctl.y() + ctl.h() / 2, open ? 2 : 1, TEXT);
            hit(HitType.SET_DROPDOWN, ctl.intersect(vp), null, m, s);
        } else if (s instanceof ColorSetting cs) {
            int pickW = 38;
            R pick = new R(ctl.right() - pickW, ctl.y(), pickW, ctl.h());
            R val = new R(ctl.x(), ctl.y(), Math.max(1, ctl.w() - pickW - 4), ctl.h());
            int col = cs.get() | 0xFF000000;
            frame(c, val, FIELD, BORDER_SOFT);
            R sw = new R(val.x() + 3, val.y() + 3, 14, Math.max(1, val.h() - 6));
            frame(c, sw, col, BORDER_SOFT);
            str(c, fit(String.format(Locale.ROOT, "#%06X", col & 0xFFFFFF), val.right() - sw.right() - 8),
                sw.right() + 5, val.y() + 3, TEXT);
            button(c, pick, "Pick", "pick:" + key, pick.contains(mx, my), picker == cs);
            hit(HitType.SET_COLOR, ctl.intersect(vp), null, m, s);
        } else if (s instanceof ItemListSetting ils) {
            boolean open = itemListEdit == ils;
            boolean ch = ctl.contains(mx, my);
            frame(c, ctl, open ? FIELD_FOCUSED : (ch ? ROW_HOVER : FIELD), open ? ACCENT : BORDER_SOFT);
            str(c, fit(ils.summary(), ctl.w() - 18), ctl.x() + 5, ctl.y() + 3, TEXT);
            chevron(c, ctl.right() - 8, ctl.y() + ctl.h() / 2, open ? 2 : 1, TEXT);
            hit(HitType.SET_ITEMS, ctl.intersect(vp), null, m, s);
            if (hov) tooltip = "Click to open item picker. Search and toggle items.";
        } else if (s instanceof StringSetting ss) {
            boolean editing = textEdit == ss;
            frame(c, ctl, editing ? FIELD_FOCUSED : (hov ? ROW_HOVER : FIELD), editing ? ACCENT : BORDER_SOFT);
            String shown = editing ? textBuf.toString() : (ss.get() == null ? "" : ss.get());
            if (editing && (System.currentTimeMillis() / 400) % 2 == 0) shown = shown + "|";
            str(c, fit(shown.isEmpty() && !editing ? "(empty)" : shown, ctl.w() - 10), ctl.x() + 5, ctl.y() + 3, editing ? TEXT : MUTED);
            hit(HitType.SET_TEXT, ctl.intersect(vp), null, m, s);
            if (hov) tooltip = "Click to edit. Enter=save, Esc=cancel. \\n = new line.";
        }
    }

    // ---------- popups ----------
    private R dropRect() {
        int widest = 0;
        for (String s : dropdown.modes()) widest = Math.max(widest, textRenderer.getWidth(s));
        int w = Math.max(dropAnchor.w(), widest + 20);
        int h = dropdown.modes().size() * ROW_H + 2;
        int x = clamp(dropAnchor.x(), 4, Math.max(4, width - 4 - w));
        int y = dropAnchor.bottom() + 1;
        if (y + h > height - 4) y = Math.max(4, dropAnchor.y() - h - 1);
        return new R(x, y, w, h);
    }

    private void renderDropdown(DrawContext c, int mx, int my) {
        R p = dropRect();
        frame(c, p, WINDOW_STRONG, BORDER_SOFT);
        hEdge(c, p.x(), p.y(), p.w(), ACCENT);
        List<String> modes = dropdown.modes();
        for (int i = 0; i < modes.size(); i++) {
            R row = new R(p.x() + 1, p.y() + 1 + i * ROW_H, p.w() - 2, ROW_H);
            boolean cur = modes.get(i).equals(dropdown.get());
            float hf = anim("dd:" + i, row.contains(mx, my) ? 1f : 0f);
            rect(c, row, lerp(0x00000000, ROW_HOVER, hf));
            if (cur) { rect(c, new R(row.x(), row.y(), 2, row.h()), ACCENT); rect(c, row, withAlpha(ACCENT, 0.14f)); }
            str(c, fit(modes.get(i), row.w() - 10), row.x() + 6, row.y() + 3, cur ? TEXT : MUTED);
        }
    }

    private R pickRect() {
        int x = clamp(pickAnchor.right() - PK_W, 4, Math.max(4, width - 4 - PK_W));
        int y = pickAnchor.bottom() + 1;
        if (y + PK_H > height - 4) y = Math.max(4, pickAnchor.y() - PK_H - 1);
        return new R(x, y, PK_W, PK_H);
    }
    private R pkSv(R p) { return new R(p.x() + PK_PAD, p.y() + PK_PAD, PK_SV_W, PK_SV_H); }
    private R pkHue(R p) { R sv = pkSv(p); return new R(sv.x(), sv.bottom() + 5, PK_SV_W, PK_HUE_H); }
    private R pkSwatch(R p, int i) { R hu = pkHue(p); return new R(hu.x() + i * (PK_SW + 2), hu.bottom() + 5, PK_SW, PK_SW); }

    private void renderPicker(DrawContext c) {
        R p = pickRect();
        frame(c, p, WINDOW_STRONG, BORDER_SOFT);
        hEdge(c, p.x(), p.y(), p.w(), ACCENT);
        R sv = pkSv(p), hu = pkHue(p);
        for (int i = 0; i < sv.w(); i++) {
            int top = hsv(pHue, i / (float) (sv.w() - 1), 1f);
            c.fillGradient(sv.x() + i, sv.y(), sv.x() + i + 1, sv.bottom(), top, 0xFF000000);
        }
        outline(c, sv.inset(-1), BORDER_SOFT);
        int mx = sv.x() + Math.round(pSat * (sv.w() - 1)), my = sv.y() + Math.round((1f - pVal) * (sv.h() - 1));
        outline(c, new R(mx - 2, my - 2, 5, 5), 0xFF000000);
        outline(c, new R(mx - 1, my - 1, 3, 3), 0xFFFFFFFF);
        for (int i = 0; i < hu.w(); i++) {
            c.fill(hu.x() + i, hu.y(), hu.x() + i + 1, hu.bottom(), hsv(i / (float) (hu.w() - 1), 1f, 1f));
        }
        outline(c, hu.inset(-1), BORDER_SOFT);
        int hx = hu.x() + Math.round(pHue * (hu.w() - 1));
        rect(c, new R(hx - 1, hu.y() - 2, 3, hu.h() + 4), 0xFF000000);
        rect(c, new R(hx, hu.y() - 2, 1, hu.h() + 4), 0xFFFFFFFF);
        int cur = picker.get() | 0xFF000000;
        for (int i = 0; i < PALETTE.length; i++) {
            R sw = pkSwatch(p, i);
            frame(c, sw, PALETTE[i], PALETTE[i] == cur ? ACCENT : BORDER_SOFT);
        }
    }


    // ---------- item list picker ----------
    private static final int IP_W = 220, IP_ROW = 16, IP_VISIBLE = 12, IP_SEARCH_H = 16, IP_PAD = 6;

    private R itemPickerRect() {
        int h = IP_PAD + IP_SEARCH_H + 4 + IP_VISIBLE * IP_ROW + IP_PAD + 18;
        int x = clamp(itemListAnchor.x() + itemListAnchor.w() - IP_W, 4, Math.max(4, width - 4 - IP_W));
        int y = itemListAnchor.bottom() + 2;
        if (y + h > height - 4) y = Math.max(4, itemListAnchor.y() - h - 2);
        return new R(x, y, IP_W, h);
    }

    private List<Item> filteredItems() {
        String q = itemSearchBuf.toString().trim().toLowerCase(Locale.ROOT);
        if (q.equals(itemFilterQuery) && !itemFilterCache.isEmpty()) return itemFilterCache;
        itemFilterQuery = q;
        List<Item> out = new ArrayList<>();
        for (Item it : Registries.ITEM) {
            if (it == Items.AIR) continue;
            var id = Registries.ITEM.getId(it);
            if (id == null) continue;
            String path = id.getPath();
            String name = it.getName(new net.minecraft.item.ItemStack(it)).getString().toLowerCase(Locale.ROOT);
            if (q.isEmpty() || path.contains(q) || name.contains(q) || id.toString().contains(q)) {
                out.add(it);
            }
        }
        // selected first
        if (itemListEdit != null) {
            out.sort((a, b) -> {
                boolean sa = itemListEdit.has(a), sb = itemListEdit.has(b);
                if (sa != sb) return sa ? -1 : 1;
                return Registries.ITEM.getId(a).getPath().compareTo(Registries.ITEM.getId(b).getPath());
            });
        }
        itemFilterCache = out;
        return out;
    }

    private void renderItemPicker(DrawContext c, int mx, int my) {
        if (itemListEdit == null) return;
        R box = itemPickerRect();
        frame(c, box, WINDOW_STRONG, ACCENT);
        hit(HitType.SET_BODY, box, null, null, itemListEdit); // absorb clicks

        // title + clear
        str(c, "Items (" + itemListEdit.count() + ")", box.x() + IP_PAD, box.y() + 4, TEXT);
        R clear = new R(box.right() - 44, box.y() + 3, 38, 12);
        button(c, clear, "Clear", "ip:clear", clear.contains(mx, my), false);

        // search
        R search = new R(box.x() + IP_PAD, box.y() + 18, box.w() - IP_PAD * 2, IP_SEARCH_H);
        frame(c, search, itemSearchFocused ? FIELD_FOCUSED : FIELD, itemSearchFocused ? ACCENT : BORDER_SOFT);
        String sq = itemSearchBuf.toString();
        if (itemSearchFocused && (System.currentTimeMillis() / 400) % 2 == 0) sq = sq + "|";
        str(c, fit(sq.isEmpty() && !itemSearchFocused ? "Search items…" : sq, search.w() - 8),
            search.x() + 4, search.y() + 4, itemSearchFocused || !itemSearchBuf.isEmpty() ? TEXT : MUTED);

        List<Item> list = filteredItems();
        int content = list.size() * IP_ROW;
        int view = IP_VISIBLE * IP_ROW;
        itemScroll = clamp(itemScroll, 0, Math.max(0, content - view));
        R listR = new R(box.x() + IP_PAD, search.bottom() + 4, box.w() - IP_PAD * 2 - (content > view ? SB : 0), view);

        c.enableScissor(listR.x(), listR.y(), listR.right(), listR.bottom());
        try {
            int first = itemScroll / IP_ROW;
            int last = Math.min(list.size() - 1, (itemScroll + view - 1) / IP_ROW);
            for (int i = first; i <= last; i++) {
                Item it = list.get(i);
                R row = new R(listR.x(), listR.y() - itemScroll + i * IP_ROW, listR.w(), IP_ROW);
                boolean hov = row.contains(mx, my);
                boolean on = itemListEdit.has(it);
                rect(c, row, hov ? ROW_HOVER : (on ? withAlpha(ACCENT, 0.25f) : ROW));
                ItemStack st = new ItemStack(it);
                c.drawItem(st, row.x() + 1, row.y());
                String name = it.getName(new net.minecraft.item.ItemStack(it)).getString();
                str(c, fit(name, row.w() - 22), row.x() + 18, row.y() + 4, on ? TEXT : MUTED);
                if (on) str(c, "✓", row.right() - 10, row.y() + 4, ACCENT);
            }
        } finally {
            c.disableScissor();
        }
        if (content > view) {
            R track = new R(box.right() - IP_PAD - SB, listR.y(), SB, view);
            drawScrollbar(c, track, content, view, itemScroll, track.contains(mx, my), false);
        }
        str(c, "Click = toggle", box.x() + IP_PAD, box.bottom() - 12, MUTED);
    }

    private void openItemPicker(ItemListSetting ils, R anchor) {
        itemListEdit = ils;
        itemListAnchor = anchor;
        itemSearchBuf.setLength(0);
        itemSearchFocused = true;
        itemScroll = 0;
        itemFilterQuery = "\\0";
        itemFilterCache = List.of();
        dropdown = null;
        picker = null;
        cancelTextEdit();
    }

    private void closeItemPicker() {
        itemListEdit = null;
        itemSearchFocused = false;
        itemSearchBuf.setLength(0);
    }

    private void openPicker(ColorSetting cs, R anchor) {
        picker = cs; pickAnchor = anchor; pDrag = 0;
        syncPickerFrom(cs.get());
    }
    private void syncPickerFrom(int argb) {
        float[] hsv = toHsv(argb);
        pHue = hsv[0]; pSat = hsv[1]; pVal = hsv[2];
    }
    private void updatePicker(double mx, double my) {
        R p = pickRect();
        if (pDrag == 1) {
            R sv = pkSv(p);
            pSat = clamp01((float) ((mx - sv.x()) / (sv.w() - 1)));
            pVal = 1f - clamp01((float) ((my - sv.y()) / (sv.h() - 1)));
        } else if (pDrag == 2) {
            R hu = pkHue(p);
            pHue = clamp01((float) ((mx - hu.x()) / (hu.w() - 1)));
        } else return;
        picker.set(hsv(pHue, pSat, pVal));
    }

    private void drawTooltip(DrawContext c, String text, int mx, int my) {
        int maxW = Math.max(32, Math.min(220, width - 16));
        List<String> lines = wrap(text, maxW);
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, textRenderer.getWidth(l));
        int w = tw + 4 * 2 + 2, h = lines.size() * 10 + 4 * 2;
        int x = mx + 8, y = my + 8;
        if (x + w > width - 4) x = mx - w - 8;
        if (y + h > height - 4) y = my - h - 8;
        x = clamp(x, 4, Math.max(4, width - 4 - w));
        y = clamp(y, 4, Math.max(4, height - 4 - h));
        rect(c, new R(x, y, w, h), WINDOW_STRONG);
        rect(c, new R(x, y, 2, h), ACCENT);
        for (int i = 0; i < lines.size(); i++) str(c, lines.get(i), x + 6, y + 4 + i * 10, TEXT);
    }

    private List<String> wrap(String text, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && textRenderer.getWidth(test) > maxW) { out.add(cur.toString()); cur = new StringBuilder(word); }
            else { cur = new StringBuilder(test); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        if (out.isEmpty()) out.add("");
        return out;
    }

    // =====================================================================
    //  Controles
    // =====================================================================
    private void drawToggle(DrawContext c, R r, String key, boolean on, boolean hover) {
        float p = anim("tg:" + key, on ? 1f : 0f);
        float e = p * p * (3f - 2f * p);
        int track = lerp(OFF_TRACK, ACCENT, e);
        if (hover) track = lift(track, 0.12f);
        pill(c, r.x(), r.y(), r.w(), r.h(), track);
        float rad = Math.max(1.5f, r.h() / 2f - 1f);
        float cxl = r.x() + 1f + rad, cxr = r.right() - 1f - rad;
        disc(c, cxl + (cxr - cxl) * e, r.y() + r.h() / 2f, rad, KNOB);
    }

    private void drawSlider(DrawContext c, R r, double ratio, boolean hovered) {
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        frame(c, r, FIELD, hovered ? BORDER : BORDER_SOFT);
        int th = Math.max(1, r.h() - 2 * Math.max(4, r.h() / 2 - 1));
        R track = new R(r.x() + 3, r.y() + (r.h() - th) / 2, Math.max(1, r.w() - 6), th);
        rect(c, track, 0xCC30333C);
        rect(c, new R(track.x(), track.y(), (int) Math.round(track.w() * ratio), track.h()), ACCENT);
        int knobX = track.x() + (int) Math.round(track.w() * ratio) - 2;
        knobX = Math.max(r.x() + 2, Math.min(r.right() - 5, knobX));
        rect(c, new R(knobX, r.y() + 2, 5, Math.max(1, r.h() - 4)), hovered ? TEXT : MUTED);
    }

    private void button(DrawContext c, R r, String label, String key, boolean hover, boolean active) {
        frame(c, r, BTN_FILL, BUTTON_BORDER);
        if (active) rect(c, r.inset(1), BTN_ACTIVE);
        float hf = anim("bt:" + key, hover ? 1f : 0f);
        if (hf > 0.001f) rect(c, r.inset(1), (Math.round(0x14 * hf) << 24) | 0x00FFFFFF);
        int tw = textRenderer.getWidth(label);
        str(c, label, r.x() + (r.w() - tw) / 2, r.y() + (r.h() - 9) / 2 + 1, TEXT);
    }

    private void drawScrollbar(DrawContext c, R track, int content, int view, int scroll, boolean hovered, boolean dragging) {
        if (content <= view) return;
        rect(c, track, 0x2EFFFFFF);
        R t = thumb(track, content, view, scroll);
        boolean active = hovered || dragging;
        int tw = active ? t.w() : Math.max(2, t.w() - 2);
        rect(c, new R(t.x() + (t.w() - tw) / 2, t.y(), tw, t.h()), active ? 0xFFC9CDD4 : 0xFF9BA0A8);
    }

    private static R thumb(R track, int content, int view, int scroll) {
        int max = Math.max(0, content - view);
        if (max <= 0) return track;
        int th = Math.min(track.h(), Math.max(12, (int) Math.round(track.h() * (view / (double) Math.max(view, content)))));
        int ty = track.y() + (int) Math.round((track.h() - th) * (scroll / (double) max));
        return new R(track.x(), ty, track.w(), th);
    }

    private static int scrollFromMouse(R track, int content, int view, int mouseY, int grab) {
        int max = Math.max(0, content - view);
        if (max <= 0) return 0;
        R t = thumb(track, content, view, 0);
        int avail = Math.max(1, track.h() - t.h());
        int y = clamp(mouseY - grab - track.y(), 0, avail);
        return (int) Math.round(max * (y / (double) avail));
    }

    // =====================================================================
    //  Primitivas de dibujo
    // =====================================================================
    private void hit(HitType t, R r, Win w, Module m, Setting<?> s) {
        if (r.w() > 0 && r.h() > 0) hits.add(new Hit(t, r, w, m, s));
    }

    private void str(DrawContext c, String s, int x, int y, int color) {
        if (s == null || s.isEmpty()) return;
        c.drawText(textRenderer, Text.literal(s).withColor(color & 0xFFFFFF), x, y, 0xFFFFFFFF, false);
    }

    private String fit(String s, int maxW) {
        if (s == null) return "";
        if (maxW <= 0) return "";
        if (textRenderer.getWidth(s) <= maxW) return s;
        int ew = textRenderer.getWidth("...");
        int end = s.length();
        while (end > 0 && textRenderer.getWidth(s.substring(0, end)) + ew > maxW) end--;
        return s.substring(0, end) + "...";
    }

    private static void rect(DrawContext c, R r, int color) {
        if (r.w() <= 0 || r.h() <= 0) return;
        c.fill(r.x(), r.y(), r.right(), r.bottom(), color);
    }
    private static void hEdge(DrawContext c, int x, int y, int w, int color) { if (w > 0) c.fill(x, y, x + w, y + 1, color); }
    private static void vEdge(DrawContext c, int x, int y, int h, int color) { if (h > 0) c.fill(x, y, x + 1, y + h, color); }

    private static void outline(DrawContext c, R r, int color) {
        if (r.w() <= 0 || r.h() <= 0) return;
        hEdge(c, r.x(), r.y(), r.w(), color);
        if (r.h() > 1) hEdge(c, r.x(), r.bottom() - 1, r.w(), color);
        if (r.h() > 2) {
            vEdge(c, r.x(), r.y() + 1, r.h() - 2, color);
            if (r.w() > 1) vEdge(c, r.right() - 1, r.y() + 1, r.h() - 2, color);
        }
    }
    private static void frame(DrawContext c, R r, int fill, int border) { rect(c, r, fill); outline(c, r, border); }

    private static void disc(DrawContext c, float cx, float cy, float r, int color) {
        int top = Math.round(cy - r), bot = Math.round(cy + r);
        for (int y = top; y < bot; y++) {
            float dy = (y + 0.5f) - cy;
            float d = r * r - dy * dy;
            if (d <= 0) continue;
            float hw = (float) Math.sqrt(d);
            int x1 = Math.round(cx - hw), x2 = Math.round(cx + hw);
            if (x2 > x1) c.fill(x1, y, x2, y + 1, color);
        }
    }

    private static void pill(DrawContext c, int x, int y, int w, int h, int color) {
        float r = h / 2f;
        int rr = h / 2;
        c.fill(x + rr, y, x + w - rr, y + h, color);
        disc(c, x + r, y + r, r, color);
        disc(c, x + w - r, y + r, r, color);
    }

    /** Flecha pequena. dir: 0 derecha, 1 abajo, 2 arriba. */
    private static void chevron(DrawContext c, int cx, int cy, int dir, int color) {
        for (int i = 0; i < 4; i++) {
            switch (dir) {
                case 0 -> {
                    c.fill(cx - 2 + i, cy - 3 + i, cx - 1 + i, cy - 2 + i, color);
                    c.fill(cx - 2 + i, cy + 3 - i, cx - 1 + i, cy + 4 - i, color);
                }
                case 1 -> {
                    c.fill(cx - 3 + i, cy - 2 + i, cx - 2 + i, cy - 1 + i, color);
                    c.fill(cx + 3 - i, cy - 2 + i, cx + 4 - i, cy - 1 + i, color);
                }
                default -> {
                    c.fill(cx - 3 + i, cy + 1 - i, cx - 2 + i, cy + 2 - i, color);
                    c.fill(cx + 3 - i, cy + 1 - i, cx + 4 - i, cy + 2 - i, color);
                }
            }
        }
    }

    private static void drawX(DrawContext c, int x, int y, int color) {
        for (int i = 0; i < 8; i++) {
            c.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            c.fill(x + 7 - i, y + i, x + 8 - i, y + i + 1, color);
        }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
        int rr = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    private static int lift(int color, float amount) {
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        r += Math.round((255 - r) * amount);
        g += Math.round((255 - g) * amount);
        b += Math.round((255 - b) * amount);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int argb, float alphaMul) {
        int a = (int) (((argb >>> 24) & 255) * Math.max(0f, Math.min(1f, alphaMul)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float hh = h * 6f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    private static float[] toHsv(int argb) {
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        h /= 6f;
        if (h < 0) h += 1f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    private static String value(Setting<?> s) {
        if (s instanceof BooleanSetting b) return b.get() ? "ON" : "OFF";
        if (s instanceof ModeSetting m) return m.get();
        if (s instanceof NumberSetting n) return String.format(Locale.ROOT, n.step() >= 1 ? "%.0f" : "%.2f", n.get());
        if (s instanceof ColorSetting cs) return String.format(Locale.ROOT, "#%06X", cs.get() & 0xFFFFFF);
        if (s instanceof ItemListSetting ils) return ils.summary();
        if (s instanceof StringSetting ss) {
            String v = ss.get() == null ? "" : ss.get();
            return v.length() > 18 ? v.substring(0, 18) + "…" : v;
        }
        return String.valueOf(s.get());
    }

    /**
     * Interpolacion suave hacia un valor objetivo.
     * Solo avanza UNA vez por frame (animFrameStamp), aunque se lea muchas veces
     * (winBounds/layout/render lo llaman en bucle y antes la animacion terminaba en 1 frame).
     */
    private float anim(String key, float target) {
        float v = anims.getOrDefault(key, target);
        if (animFrameStamp.getOrDefault(key, -1L) != animFrame) {
            // expand/collapse un poco mas lento para que se note
            float speed = key.startsWith("exp:") ? 8f
                : key.startsWith("setOpen:") ? 10f
                : 16f;
            v += (target - v) * Math.min(1f, dt * speed);
            if (Math.abs(target - v) < 0.002f) v = target;
            anims.put(key, v);
            animFrameStamp.put(key, animFrame);
        }
        return v;
    }

    // =====================================================================
    //  Input
    // =====================================================================
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int b = click.button();

        if (dropdown != null) {
            R p = dropRect();
            if (p.contains(mx, my) && b == 0) {
                int i = (int) ((my - p.y() - 1) / ROW_H);
                List<String> ms = dropdown.modes();
                if (i >= 0 && i < ms.size()) dropdown.set(ms.get(i));
            }
            dropdown = null;
            return true;
        }
        if (picker != null) {
            R p = pickRect();
            if (!p.contains(mx, my)) { picker = null; return true; }
            if (b != 0) return true;
            if (pkSv(p).contains(mx, my)) { pDrag = 1; updatePicker(mx, my); return true; }
            if (pkHue(p).inset(-1).contains(mx, my)) { pDrag = 2; updatePicker(mx, my); return true; }
            for (int i = 0; i < PALETTE.length; i++) {
                if (pkSwatch(p, i).contains(mx, my)) { picker.set(PALETTE[i]); syncPickerFrom(PALETTE[i]); return true; }
            }
            return true;
        }

        if (itemListEdit != null) {
            R box = itemPickerRect();
            if (!box.contains(mx, my)) { closeItemPicker(); return true; }
            if (b != 0) return true;
            // clear
            R clear = new R(box.right() - 44, box.y() + 3, 38, 12);
            if (clear.contains(mx, my)) {
                itemListEdit.clear();
                itemFilterQuery = "\0";
                return true;
            }
            R search = new R(box.x() + IP_PAD, box.y() + 18, box.w() - IP_PAD * 2, IP_SEARCH_H);
            if (search.contains(mx, my)) {
                itemSearchFocused = true;
                return true;
            }
            itemSearchFocused = false;
            List<Item> list = filteredItems();
            int content = list.size() * IP_ROW;
            int view = IP_VISIBLE * IP_ROW;
            R listR = new R(box.x() + IP_PAD, search.bottom() + 4, box.w() - IP_PAD * 2 - (content > view ? SB : 0), view);
            if (listR.contains(mx, my)) {
                int idx = (int) ((my - listR.y() + itemScroll) / IP_ROW);
                if (idx >= 0 && idx < list.size()) {
                    Item it = list.get(idx);
                    var id = Registries.ITEM.getId(it);
                    if (id != null) {
                        itemListEdit.toggle(id.toString());
                        itemFilterQuery = "\0"; // refresh sort
                    }
                }
                return true;
            }
            return true;
        }

        // click fuera del text edit / search -> guardar
        if (textEdit != null) {
            boolean onText = false;
            for (Hit h : hits) {
                if (h.type() == HitType.SET_TEXT && h.setting() == textEdit && h.r().contains(mx, my)) { onText = true; break; }
            }
            if (!onText) commitTextEdit();
        }
        if (searchFocused) {
            boolean on = false;
            for (Hit h : hits) if (h.type() == HitType.SEARCH_FIELD && h.r().contains(mx, my)) { on = true; break; }
            if (!on) { searchQuery = searchBuf.toString(); searchFocused = false; }
        }
        if (settingsSearchFocused) {
            boolean on = false;
            for (Hit h : hits) if (h.type() == HitType.SET_SEARCH && h.r().contains(mx, my)) { on = true; break; }
            if (!on) { settingsSearch = settingsSearchBuf.toString(); settingsSearchFocused = false; }
        }

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.r().contains(mx, my)) continue;
            switch (h.type()) {
                case WIN_BODY -> bringToFront(h.win());
                case WIN_HEADER -> {
                    bringToFront(h.win());
                    if (b == 0) {
                        dragWin = h.win();
                        dragOffX = (int) mx - h.r().x(); dragOffY = (int) my - h.r().y();
                        dragStartX = (int) mx; dragStartY = (int) my;
                        dragMoved = false;
                    } else if (b == 1) {
                        h.win().collapsed = !h.win().collapsed;
                    }
                }
                case WIN_SCROLLBAR -> startScrollbarDrag(h.win(), h.r(), winContentH(h.win()),
                    winBounds(h.win()).h() - HEAD - 1, h.win().scroll, my);
                case MODULE -> {
                    if (h.win() != null) bringToFront(h.win());
                    if (b == 0) h.module().toggle();
                    else if (b == 1 && !h.module().settings().isEmpty()) {
                        selected = h.module(); setScroll = 0; settingsSearch = ""; settingsSearchFocused = false; closeItemPicker(); anims.put("setOpen:" + selected.name(), 0f);
                    }
                    else if (b == 2) { bindingModule = h.module(); } // middle-click = bind (Autism)
                }
                case MODULE_BIND -> {
                    if (b == 0) bindingModule = h.module();
                }
                case SEARCH_FIELD -> {
                    if (h.win() != null) bringToFront(h.win());
                    if (b == 0) {
                        searchFocused = true;
                        searchBuf.setLength(0);
                        searchBuf.append(searchQuery == null ? "" : searchQuery);
                        settingsSearchFocused = false;
                        cancelTextEdit();
                    }
                }
                case SET_BODY -> { }
                case SET_CLOSE -> { cancelTextEdit(); bindingModule = null; settingsSearchFocused = false; closeItemPicker(); selected = null; }
                case SET_ENABLED -> { if (b == 0) h.module().toggle(); }
                case SET_BIND -> { if (b == 0) bindingModule = h.module(); }
                case SET_SEARCH -> {
                    if (b == 0) {
                        settingsSearchFocused = true;
                        settingsSearchBuf.setLength(0);
                        settingsSearchBuf.append(settingsSearch == null ? "" : settingsSearch);
                        searchFocused = false;
                        cancelTextEdit();
                    }
                }
                case SET_RESET -> { if (b == 0) for (Setting<?> s : h.module().settings()) s.reset(); }
                case SET_BOOL -> { if (b == 0 && h.setting() instanceof BooleanSetting bs) bs.toggle(); }
                case SET_SLIDER -> {
                    if (b == 0 && h.setting() instanceof NumberSetting n) { sliderSetting = n; setSlider(n, mx); }
                }
                case SET_DROPDOWN -> {
                    if (b == 0 && h.setting() instanceof ModeSetting ms) { dropdown = ms; dropAnchor = h.r(); }
                }
                case SET_COLOR -> {
                    if (b == 0 && h.setting() instanceof ColorSetting cs) openPicker(cs, h.r());
                }
                case SET_ITEMS -> {
                    if (b == 0 && h.setting() instanceof ItemListSetting ils) {
                        if (itemListEdit == ils) closeItemPicker();
                        else openItemPicker(ils, h.r());
                    }
                }
                case SET_TEXT -> {
                    if (b == 0 && h.setting() instanceof StringSetting ss) {
                        textEdit = ss;
                        textBuf.setLength(0);
                        textBuf.append(ss.get() == null ? "" : ss.get());
                    }
                }
                case SET_SCROLLBAR -> startScrollbarDrag(null, h.r(), setContent, setView, setScroll, my);
            }
            return true;
        }
        if (selected != null) { cancelTextEdit(); selected = null; return true; }   // clic fuera de la ventana de ajustes
        return super.mouseClicked(click, doubled);
    }

    private void startScrollbarDrag(Win w, R track, int content, int view, int scroll, double my) {
        sbActive = true; sbWin = w; sbTrack = track; sbContent = content; sbView = view;
        R t = thumb(track, content, view, scroll);
        sbGrab = t.contains(track.x(), my) ? (int) my - t.y() : t.h() / 2;
        applyScrollbar(my);
    }

    private void applyScrollbar(double my) {
        int s = scrollFromMouse(sbTrack, sbContent, sbView, (int) my, sbGrab);
        if (sbWin != null) sbWin.scroll = s; else setScroll = s;
    }

    private void setSlider(NumberSetting n, double mouseX) {
        R t = sliderRects.get(n);
        if (t == null) return;
        double f = Math.max(0, Math.min(1, (mouseX - t.x()) / t.w()));
        double v = n.min() + f * (n.max() - n.min());
        if (n.step() > 0) v = n.min() + Math.round((v - n.min()) / n.step()) * n.step();
        v = Math.max(n.min(), Math.min(n.max(), v));
        n.set(Math.round(v * 10000.0) / 10000.0);
    }

    @Override public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mx = click.x(), my = click.y();
        if (picker != null && pDrag != 0) { updatePicker(mx, my); return true; }
        if (sbActive) { applyScrollbar(my); return true; }
        if (dragWin != null) {
            if (Math.abs(mx - dragStartX) + Math.abs(my - dragStartY) > 3) dragMoved = true;
            if (dragMoved) { dragWin.x = (int) mx - dragOffX; dragWin.y = (int) my - dragOffY; customLayout = true; }
            return true;
        }
        if (sliderSetting instanceof NumberSetting n) { setSlider(n, mx); return true; }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override public boolean mouseReleased(Click click) {
        if (dragWin != null && !dragMoved) dragWin.collapsed = !dragWin.collapsed;   // clic en cabecera = plegar
        dragWin = null; sliderSetting = null; sbActive = false; sbWin = null; pDrag = 0;
        return super.mouseReleased(click);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (dropdown != null || picker != null) return true;
        if (itemListEdit != null) {
            itemScroll = Math.max(0, itemScroll - (int) Math.round(vertical * IP_ROW * 3));
            return true;
        }
        if (selected != null) {
            setScroll = Math.max(0, setScroll - (int) Math.round(vertical * (SET_H + SET_GAP)));
            return true;
        }
        Win w = winAt(mx, my);
        if (w != null && w.cat != null && !w.collapsed) w.scroll = Math.max(0, w.scroll - (int) Math.round(vertical * ROW_H));
        return true;
    }

    private void commitTextEdit() {
        if (textEdit == null) return;
        textEdit.set(textBuf.toString());
        textEdit = null;
    }

    private void cancelTextEdit() {
        textEdit = null;
        textBuf.setLength(0);
    }

    @Override public boolean charTyped(CharInput input) {
        if (bindingModule != null) return true;
        if (textEdit != null && input.isValidChar()) {
            String s = input.asString();
            if (s != null && !s.isEmpty()) {
                if (textBuf.length() + s.length() <= textEdit.maxLen()) textBuf.append(s);
                return true;
            }
        }
        if (itemListEdit != null && itemSearchFocused && input.isValidChar()) {
            String s = input.asString();
            if (s != null && !s.isEmpty() && itemSearchBuf.length() < 48) {
                itemSearchBuf.append(s);
                itemFilterQuery = "\0";
                itemScroll = 0;
            }
            return true;
        }
        if (searchFocused && input.isValidChar()) {
            String s = input.asString();
            if (s != null && !s.isEmpty() && searchBuf.length() < 64) searchBuf.append(s);
            return true;
        }
        if (settingsSearchFocused && input.isValidChar()) {
            String s = input.asString();
            if (s != null && !s.isEmpty() && settingsSearchBuf.length() < 64) settingsSearchBuf.append(s);
            return true;
        }
        return super.charTyped(input);
    }

    @Override public boolean keyPressed(KeyInput input) {
        // R = reordenar ventanas (layout Autism)
        if (!searchFocused && !settingsSearchFocused && textEdit == null && itemListEdit == null
            && bindingModule == null && input.key() == GLFW.GLFW_KEY_R && !input.hasCtrl()) {
            customLayout = false;
            layoutWindows();
            return true;
        }

        int k = input.key();

        // Autism keybind capture
        if (bindingModule != null) {
            if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) {
                bindingModule.setKeybind(-1);
            } else {
                bindingModule.setKeybind(k);
            }
            bindingModule = null;
            return true;
        }

        if (textEdit != null) {
            if (k == GLFW.GLFW_KEY_ESCAPE) { cancelTextEdit(); return true; }
            if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) {
                if (input.hasShift()) {
                    if (textBuf.length() < textEdit.maxLen()) textBuf.append('\n');
                } else commitTextEdit();
                return true;
            }
            if (k == GLFW.GLFW_KEY_BACKSPACE) {
                if (!textBuf.isEmpty()) textBuf.deleteCharAt(textBuf.length() - 1);
                return true;
            }
            return true;
        }

        if (searchFocused) {
            if (k == GLFW.GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
            if (k == GLFW.GLFW_KEY_ENTER) { searchQuery = searchBuf.toString(); searchFocused = false; return true; }
            if (k == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchBuf.isEmpty()) searchBuf.deleteCharAt(searchBuf.length() - 1);
                return true;
            }
            return true;
        }
        if (settingsSearchFocused) {
            if (k == GLFW.GLFW_KEY_ESCAPE) { settingsSearchFocused = false; return true; }
            if (k == GLFW.GLFW_KEY_ENTER) { settingsSearch = settingsSearchBuf.toString(); settingsSearchFocused = false; return true; }
            if (k == GLFW.GLFW_KEY_BACKSPACE) {
                if (!settingsSearchBuf.isEmpty()) settingsSearchBuf.deleteCharAt(settingsSearchBuf.length() - 1);
                return true;
            }
            return true;
        }

        if (itemListEdit != null) {
            if (k == GLFW.GLFW_KEY_ESCAPE) { closeItemPicker(); return true; }
            if (itemSearchFocused && k == GLFW.GLFW_KEY_BACKSPACE) {
                if (!itemSearchBuf.isEmpty()) {
                    itemSearchBuf.deleteCharAt(itemSearchBuf.length() - 1);
                    itemFilterQuery = "\0";
                    itemScroll = 0;
                }
                return true;
            }
            return true;
        }

        if (k == GLFW.GLFW_KEY_ESCAPE) {
            if (dropdown != null) dropdown = null;
            else if (picker != null) picker = null;
            else if (itemListEdit != null) closeItemPicker();
            else if (selected != null) { settingsSearchFocused = false; selected = null; }
            else close();
            return true;
        }
        if (k == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(input);
    }
}
