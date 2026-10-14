package com.devs.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layouts de todos los elementos del HUD customizables (modulos + scoreboard vanilla).
 * Posiciones en fracciones de pantalla; se guardan en config/devs/hud.json.
 */
public final class HudLayoutManager {
    private static final Logger LOG = LoggerFactory.getLogger("devs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String HUD_INFO = "hud_info";
    public static final String POTION_HUD = "potion_hud";
    public static final String ARMOR_HUD = "armor_hud";
    public static final String HIT_LOG = "hit_log";
    public static final String SCOREBOARD = "scoreboard";
    public static final String CUSTOM_DISPLAY = "custom_display";
    public static final String MODULE_DISPLAY = "module_display";
    public static final String CONSUMABLE_SELF = "consumable_self";
    public static final String CONSUMABLE_ENEMY = "consumable_enemy";
    public static final String TARGET_HUD = "target_hud";
    public static final String NOTIFICATIONS = "notifications";
    public static final String RADAR = "radar";
    public static final String NETWORK_HUD = "network_hud";
    public static final String TARGET_INV = "target_inv";
    public static final String BLOCK_COUNTER = "block_counter";
    public static final String CRYSTAL_SPEED = "crystal_speed";
    public static final String WAYPOINT_HUD = "waypoint_hud";
    public static final String PEEK_INV = "peek_inv";
    public static final String SESSION_STATS = "session_stats";
    public static final String SPEEDOMETER = "speedometer";
    public static final String REACH_DISPLAY = "reach_display";
    public static final String COMBO_COUNTER = "combo_counter";
    public static final String WORLD_CLOCK = "world_clock";
    public static final String ELYTRA_HUD = "elytra_hud";
    public static final String LIGHT_LEVEL = "light_level";
    public static final String TOTEM_INDICATOR = "totem_indicator";
    public static final String HOTBAR_WARNING = "hotbar_warning";
    public static final String KEYBIND_LIST = "keybind_list";
    public static final String SLIME_CHUNKS = "slime_chunks";
    public static final String PING_GRAPH = "ping_graph";
    public static final String IMAGE_DISPLAY = "image_display";

    private static final Map<String, HudLayout> LAYOUTS = new LinkedHashMap<>();
    private static boolean scoreboardPushed;

    static {
        // Left column — info
        register(new HudLayout(WORLD_CLOCK, "World Clock", 0.01f, 0.04f, 1f, false));
        register(new HudLayout(HUD_INFO, "HUD Info", 0.01f, 0.09f, 1f, false));
        register(new HudLayout(SLIME_CHUNKS, "Slime Chunks", 0.01f, 0.22f, 1f, false));
        register(new HudLayout(LIGHT_LEVEL, "Light Level", 0.01f, 0.27f, 1f, false));
        register(new HudLayout(NETWORK_HUD, "Network HUD", 0.01f, 0.32f, 1f, false));
        register(new HudLayout(PING_GRAPH, "Ping Graph", 0.01f, 0.38f, 1f, false));
        register(new HudLayout(CONSUMABLE_SELF, "Consumable (You)", 0.01f, 0.48f, 1f, false));
        register(new HudLayout(CONSUMABLE_ENEMY, "Consumable (Enemy)", 0.01f, 0.56f, 1f, false));
        register(new HudLayout(SESSION_STATS, "Session Stats", 0.01f, 0.64f, 1f, false));
        register(new HudLayout(SPEEDOMETER, "Speedometer", 0.01f, 0.72f, 1f, false));
        register(new HudLayout(ELYTRA_HUD, "Elytra HUD", 0.01f, 0.77f, 1f, false));
        // Center
        register(new HudLayout(REACH_DISPLAY, "Reach Display", 0.40f, 0.04f, 1f, false));
        register(new HudLayout(BLOCK_COUNTER, "Block Counter", 0.40f, 0.10f, 1f, false));
        register(new HudLayout(WAYPOINT_HUD, "Waypoint HUD", 0.40f, 0.18f, 1f, false));
        register(new HudLayout(CUSTOM_DISPLAY, "Custom Display", 0.35f, 0.40f, 1f, false));
        register(new HudLayout(COMBO_COUNTER, "Combo Counter", 0.42f, 0.55f, 1f, false));
        register(new HudLayout(NOTIFICATIONS, "Notifications", 0.38f, 0.72f, 1f, false));
        register(new HudLayout(TARGET_HUD, "Target HUD", 0.38f, 0.80f, 1f, false));
        register(new HudLayout(TOTEM_INDICATOR, "Totem Indicator", 0.40f, 0.90f, 1f, false));
        // Right
        register(new HudLayout(SCOREBOARD, "Scoreboard", 1.0f, 0.0f, 1f, true));
        register(new HudLayout(MODULE_DISPLAY, "Module Display", 0.82f, 0.08f, 1f, false));
        register(new HudLayout(HIT_LOG, "Hit Log", 0.82f, 0.30f, 1f, false));
        register(new HudLayout(KEYBIND_LIST, "Keybind List", 0.82f, 0.50f, 1f, false));
        register(new HudLayout(ARMOR_HUD, "Armor HUD", 0.90f, 0.68f, 1f, false));
        register(new HudLayout(HOTBAR_WARNING, "Hotbar Warning", 0.72f, 0.78f, 1f, false));
        register(new HudLayout(POTION_HUD, "Potion HUD", 0.82f, 0.88f, 1f, false));
        register(new HudLayout(IMAGE_DISPLAY, "Image Display", 0.55f, 0.35f, 1f, false));
    }

    private HudLayoutManager() {}

    private static void register(HudLayout l) { LAYOUTS.put(l.id, l); }

    public static HudLayout get(String id) { return LAYOUTS.get(id); }

    public static Collection<HudLayout> all() { return Collections.unmodifiableCollection(LAYOUTS.values()); }

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("hud.json");
    }

    public static void load() {
        Path f = file();
        if (!Files.isRegularFile(f)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (HudLayout l : LAYOUTS.values()) {
                JsonElement el = root.get(l.id);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                if (o.has("x")) l.x = o.get("x").getAsFloat();
                if (o.has("y")) l.y = o.get("y").getAsFloat();
                if (o.has("scale")) l.scale = o.get("scale").getAsFloat();
                l.clamp();
            }
        } catch (Exception e) {
            LOG.error("[DEVS] Could not load HUD layout", e);
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (HudLayout l : LAYOUTS.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("x", l.x);
                o.addProperty("y", l.y);
                o.addProperty("scale", l.scale);
                root.add(l.id, o);
            }
            Path dir = file().getParent();
            Files.createDirectories(dir);
            Path tmp = dir.resolve("hud.json.tmp");
            Files.writeString(tmp, GSON.toJson(root));
            Files.move(tmp, file(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.error("[DEVS] Could not save HUD layout", e);
        }
    }

    public static void resetAll() {
        for (HudLayout l : LAYOUTS.values()) l.reset();
        save();
    }

    // ------------------------------------------------------------------ transform de modulos

    /**
     * Aplica la transformacion del layout y deja el origen en la esquina superior-izquierda del panel.
     * {@code contentW}/{@code contentH} son el tamano del contenido a escala 1.
     */
    public static void begin(DrawContext ctx, String id, int contentW, int contentH) {
        HudLayout l = LAYOUTS.get(id);
        if (l == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        float px = l.x * sw;
        float py = l.y * sh;
        // No dejar que se salga del todo de la pantalla.
        float maxX = Math.max(0, sw - contentW * l.scale);
        float maxY = Math.max(0, sh - contentH * l.scale);
        px = Math.max(0, Math.min(maxX, px));
        py = Math.max(0, Math.min(maxY, py));

        l.lastW = Math.max(1, contentW);
        l.lastH = Math.max(1, contentH);
        l.lastPx = px;
        l.lastPy = py;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(px, py);
        m.scale(l.scale, l.scale);
    }

    public static void end(DrawContext ctx) {
        ctx.getMatrices().popMatrix();
    }

    // ------------------------------------------------------------------ scoreboard vanilla

    /**
     * Prepara la matriz para mover/escalar el scoreboard vanilla.
     * Ancla = esquina superior-derecha del panel (x/y en fracciones de pantalla).
     * Default (x=1, y=0, scale=1) = sin transformar (posicion vanilla exacta).
     */
    public static void pushScoreboard(DrawContext ctx, ScoreboardObjective objective) {
        scoreboardPushed = false;
        HudLayout l = LAYOUTS.get(SCOREBOARD);
        if (l == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        int[] size = measureScoreboard(mc, objective);
        int contentW = size[0], contentH = size[1];
        float s = l.scale;

        boolean identity = Math.abs(s - 1f) < 0.001f
            && Math.abs(l.x - 1f) < 0.001f
            && Math.abs(l.y) < 0.001f;

        if (identity) {
            // Sin transform: bounds aproximados de vanilla (derecha arriba).
            l.lastW = contentW;
            l.lastH = contentH;
            l.lastPx = sw - contentW;
            l.lastPy = 0;
            return;
        }

        float right = l.x * sw;
        float top = l.y * sh;
        right = Math.max(contentW * s * 0.25f, Math.min(sw + contentW * s * 0.5f, right));
        top = Math.max(-contentH * s * 0.5f, Math.min(sh - contentH * s * 0.25f, top));

        l.lastW = contentW;
        l.lastH = contentH;
        l.lastPx = right - contentW * s;
        l.lastPy = top;

        // Mapea el punto vanilla (sw, 0) -> (right, top) y aplica escala.
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(right, top);
        m.scale(s, s);
        m.translate(-sw, 0f);
        scoreboardPushed = true;
    }

    public static void popScoreboard(DrawContext ctx) {
        if (!scoreboardPushed) return;
        scoreboardPushed = false;
        ctx.getMatrices().popMatrix();
    }

    /** Estima ancho/alto del scoreboard como vanilla para el editor y el clamp. */
    private static int[] measureScoreboard(MinecraftClient mc, ScoreboardObjective objective) {
        var tr = mc.textRenderer;
        int max = tr.getWidth(objective.getDisplayName());
        Scoreboard board = objective.getScoreboard();
        Collection<ScoreboardEntry> entries = board.getScoreboardEntries(objective);
        int rows = 0;
        for (ScoreboardEntry e : entries) {
            Text base = e.display() != null ? e.display() : Text.literal(e.owner());
            Team team = board.getScoreHolderTeam(e.owner());
            Text name = Team.decorateName(team, base);
            int scoreW = tr.getWidth(" " + Integer.toString(e.value()));
            max = Math.max(max, tr.getWidth(name) + scoreW);
            rows++;
            if (rows >= 15) break; // vanilla limita la lista visible
        }
        int w = max + 6; // padding similar a vanilla
        int h = (rows + 1) * 9 + 2;
        return new int[]{Math.max(40, w), Math.max(18, h)};
    }

    /** Actualiza last* del scoreboard aunque no se este dibujando (editor). */
    public static void refreshScoreboardBounds() {
        HudLayout l = LAYOUTS.get(SCOREBOARD);
        if (l == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            l.lastW = 80; l.lastH = 40;
            l.lastPx = l.x * mc.getWindow().getScaledWidth() - l.lastW * l.scale;
            l.lastPy = l.y * mc.getWindow().getScaledHeight();
            return;
        }
        Scoreboard board = mc.world.getScoreboard();
        ScoreboardObjective obj = board.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        if (obj == null) {
            l.lastW = 80; l.lastH = 40;
            l.lastPx = l.x * sw - l.lastW * l.scale;
            l.lastPy = l.y * sh;
            return;
        }
        int[] size = measureScoreboard(mc, obj);
        l.lastW = size[0]; l.lastH = size[1];
        float right = l.x * sw, top = l.y * sh;
        l.lastPx = right - size[0] * l.scale;
        l.lastPy = top;
    }

    /** Mueve el layout desde coordenadas de pantalla (pixeles del ancla). */
    public static void moveFromScreen(HudLayout l, float screenX, float screenY, int sw, int sh) {
        if (l.scoreboard) {
            // screenX/Y = esquina superior-derecha
            l.x = sw <= 0 ? 0 : screenX / sw;
            l.y = sh <= 0 ? 0 : screenY / sh;
        } else {
            l.x = sw <= 0 ? 0 : screenX / sw;
            l.y = sh <= 0 ? 0 : screenY / sh;
        }
        l.clamp();
    }
}
