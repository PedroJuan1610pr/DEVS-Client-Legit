package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Toasts en pantalla: pops, death, módulo on/off, custom. */
public final class Notifications extends Module {
    private record Toast(String title, String body, int color, int born, int life) {}

    private static Notifications INSTANCE;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<Toast> toasts = new ArrayList<>();
    private int now;

    public final BooleanSetting moduleToggle = add(new BooleanSetting("Module Toggle", true));
    public final BooleanSetting totemPops = add(new BooleanSetting("Totem Pops", true));
    public final BooleanSetting deaths = add(new BooleanSetting("Deaths", true));
    public final BooleanSetting selfDeath = add(new BooleanSetting("Self Death", true));
    public final BooleanSetting lowTotem = add(new BooleanSetting("Low Totem", true));
    public final NumberSetting duration = add(new NumberSetting("Duration (s)", 3.0, 1.0, 8.0, 0.5));
    public final NumberSetting max = add(new NumberSetting("Max Visible", 5, 1, 10, 1));
    public final ColorSetting accent = add(new ColorSetting("Accent", 0xFF4DD2FF));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));

    private int lastTotemCount = -1;
    /** last known alive state per player UUID (null = never seen). */
    private final Map<UUID, Boolean> wasAlive = new HashMap<>();

    public Notifications() {
        super("Notifications", "On-screen toasts for events (pops, death, modules…).", "Misc");
        INSTANCE = this;
    }

    public static void push(String title, String body, int color) {
        if (INSTANCE == null || !INSTANCE.enabled()) return;
        int life = (int) (INSTANCE.duration.get() * 20);
        INSTANCE.toasts.add(0, new Toast(title, body == null ? "" : body, color | 0xFF000000, INSTANCE.now, life));
        while (INSTANCE.toasts.size() > 20) INSTANCE.toasts.remove(INSTANCE.toasts.size() - 1);
    }

    public static void push(String title, String body) {
        push(title, body, 0xFF4DD2FF);
    }

    /** Totem pop toast (respects the Totem Pops setting). */
    public static void pushTotemPop(String playerName) {
        if (INSTANCE == null || !INSTANCE.enabled() || !INSTANCE.totemPops.get()) return;
        push("Totem Pop", playerName, 0xFFFFD94D);
    }

    /** Death toast (respects the Deaths setting). */
    /** UUIDs already toasted this tick-window to avoid double Death toasts. */
    private final Map<String, Integer> recentDeathToast = new HashMap<>();

    public static void pushDeath(String playerName, String extra) {
        if (INSTANCE == null || !INSTANCE.enabled() || !INSTANCE.deaths.get()) return;
        String key = playerName.toLowerCase();
        Integer last = INSTANCE.recentDeathToast.get(key);
        if (last != null && INSTANCE.now - last < 40) return; // ~2s dedupe
        INSTANCE.recentDeathToast.put(key, INSTANCE.now);
        String body = extra == null || extra.isEmpty() ? playerName : playerName + " · " + extra;
        push("Death", body, 0xFFFF4D6D);
    }

    public static boolean moduleToggleEnabled() {
        return INSTANCE != null && INSTANCE.enabled() && INSTANCE.moduleToggle.get();
    }

    @Override public void tick() {
        now++;
        toasts.removeIf(t -> now - t.born() > t.life());
        if (!enabled() || mc.player == null) return;

        if (lowTotem.get()) {
            int tots = 0;
            var inv = mc.player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                if (inv.getStack(i).is(net.minecraft.item.Items.TOTEM_OF_UNDYING))
                    tots += inv.getStack(i).getCount();
            }
            if (lastTotemCount >= 0 && tots < lastTotemCount && tots <= 1) {
                push("Low Totem", tots + " left", 0xFFFF4D6D);
            }
            lastTotemCount = tots;
        }

        // ---- Player death detection (any nearby player, including self) ----
        if (deaths.get() && mc.world != null) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                boolean alive = p.isAlive() && !p.isDead() && p.getHealth() > 0f;
                UUID id = p.getUuid();
                Boolean prev = wasAlive.put(id, alive);
                if (prev != null && prev && !alive) {
                    boolean isSelf = p == mc.player;
                    if (isSelf && !selfDeath.get()) continue;
                    String name = isSelf ? "You" : p.getName().getString();
                    pushDeath(name, isSelf ? "died" : "died");
                }
            }
            // drop entries for players no longer in the world
            if (now % 40 == 0) {
                Iterator<Map.Entry<UUID, Boolean>> it = wasAlive.entrySet().iterator();
                while (it.hasNext()) {
                    UUID id = it.next().getKey();
                    if (mc.world.getPlayerByUuid(id) == null) it.remove();
                }
            }
        }
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || toasts.isEmpty()) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        var tr = mc.textRenderer;
        int maxN = max.get().intValue();
        int shown = Math.min(maxN, toasts.size());
        int maxW = 120;
        for (int i = 0; i < shown; i++) {
            Toast t = toasts.get(i);
            maxW = Math.max(maxW, tr.getWidth(t.title()) + 12);
            if (!t.body().isEmpty()) maxW = Math.max(maxW, tr.getWidth(t.body()) + 12);
        }
        int rowH = 24;
        int contentW = maxW + 8;
        int contentH = shown * rowH + 4;
        HudLayoutManager.begin(ctx, HudLayoutManager.NOTIFICATIONS, contentW, contentH);
        int y = 2;
        for (int i = 0; i < shown; i++) {
            Toast t = toasts.get(i);
            float age = (now - t.born()) / (float) Math.max(1, t.life());
            // slide-in first ~8 ticks, fade-out last 30%
            float slide = Math.min(1f, (now - t.born()) / 8f);
            float fade = age > 0.7f ? Math.max(0f, 1f - (age - 0.7f) / 0.3f) : 1f;
            int alpha = (int) (fade * (0x20 + slide * 0xA0));
            alpha = Math.max(0x18, Math.min(0xC0, alpha));
            int slideX = (int) ((1f - slide) * 18f);

            if (background.get()) {
                ctx.fill(slideX, y, contentW, y + rowH - 2, (alpha << 24));
                ctx.fill(slideX, y, slideX + 2, y + rowH - 2, t.color());
            }
            int textA = (int) (fade * 0xFF) << 24;
            ctx.drawTextWithShadow(tr, t.title(), slideX + 6, y + 3, (t.color() & 0x00FFFFFF) | textA);
            if (!t.body().isEmpty())
                ctx.drawTextWithShadow(tr, t.body(), slideX + 6, y + 12, 0x00CCCCCC | textA);
            y += rowH;
        }
        HudLayoutManager.end(ctx);
    }
}
