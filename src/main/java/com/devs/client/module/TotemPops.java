package com.devs.client.module;

import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cuenta los tótems que revientan los jugadores (el servidor manda el estado 35 de la entidad).
 * El contador sobrevive a que el jugador salga de tu distancia de render: solo se borra si muere
 * (viéndolo), se desconecta del servidor, o pasa mucho tiempo sin que lo veas.
 */
public final class TotemPops extends Module {
    private record Popup(Vec3d pos, String text, int born) {}

    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting chat = add(new BooleanSetting("Chat", true));
    public final BooleanSetting floating = add(new BooleanSetting("Floating Text", true));
    public final BooleanSetting self = add(new BooleanSetting("Include Self", true));
    public final BooleanSetting deathMsg = add(new BooleanSetting("Death Message", true));
    public final NumberSetting duration = add(new NumberSetting("Duration (s)", 2.0, 0.5, 6.0, 0.5));
    public final NumberSetting forgetAfter = add(new NumberSetting("Forget After (s)", 300.0, 10.0, 3600.0, 10.0));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFD94D));
    public final BooleanSetting resetBtn = add(new BooleanSetting("Clear Counters", false));

    private final Map<UUID, Integer> pops = new HashMap<>();
    private final Map<UUID, Integer> lastSeen = new HashMap<>();   // tick en el que lo viste por última vez
    private final List<Popup> popups = new ArrayList<>();
    private int now;

    public TotemPops() { super("Totem Pops", "Counts and announces totems popped by players.", "Combat"); }

    /** Llamado desde TotemPopMixin cuando llega el estado 35 de un jugador. */
    public void onPop(PlayerEntity p) {
        if (!enabled()) return;
        if (p == mc.player && !self.get()) return;
        int n = pops.merge(p.getUuid(), 1, Integer::sum);
        lastSeen.put(p.getUuid(), now);
        String name = p.getName().getString();
        if (floating.get()) popups.add(new Popup(new Vec3d(p.getX(), p.getBoundingBox().maxY + 0.9, p.getZ()), "Pop x" + n, now));
        Notifications.pushTotemPop(name + " (x" + n + ")");
        if (chat.get() && mc.player != null)
            mc.player.sendMessage(Text.literal("[DEVS] " + name + " popped a totem (x" + n + ")").formatted(Formatting.GOLD));
    }

    public int popsOf(UUID id) { return enabled() ? pops.getOrDefault(id, 0) : 0; }

    @Override protected void onDisable() { pops.clear(); lastSeen.clear(); popups.clear(); }

    @Override public void tick() {
        now++;
        if (!enabled()) return;

        if (resetBtn.get()) {                       // el botón "Clear Counters"
            pops.clear(); lastSeen.clear();
            resetBtn.set(false);
        }

        int life = (int) (duration.get() * 20);
        popups.removeIf(p -> now - p.born() > life);

        if (mc.world == null || mc.getNetworkHandler() == null) { pops.clear(); lastSeen.clear(); return; }
        if (pops.isEmpty()) return;

        int forget = (int) (forgetAfter.get() * 20);
        Iterator<Map.Entry<UUID, Integer>> it = pops.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            UUID id = en.getKey();
            PlayerEntity pl = mc.world.getPlayerByUuid(id);

            if (pl == null) {
                // No está en tu render: NO se borra. Solo si se desconectó o lleva demasiado tiempo sin verse.
                boolean online = mc.getNetworkHandler().getPlayerListEntry(id) != null;
                if (!online || now - lastSeen.getOrDefault(id, now) > forget) {
                    lastSeen.remove(id);
                    it.remove();
                }
                continue;
            }

            lastSeen.put(id, now);
            if (!pl.isAlive()) {
                int n = en.getValue();
                String name = pl.getName().getString();
                if (deathMsg.get()) {
                    Notifications.pushDeath(name, n + " pop" + (n == 1 ? "" : "s"));
                    if (chat.get() && mc.player != null)
                        mc.player.sendMessage(Text.literal("[DEVS] " + name + " died after " + n
                            + " pop" + (n == 1 ? "" : "s")).formatted(Formatting.RED));
                }
                lastSeen.remove(id);
                it.remove();
            }
        }
    }

    @Override public void render() {
        if (!floating.get() || popups.isEmpty() || mc.player == null) return;
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        double life = Math.max(1, duration.get() * 20);
        for (Popup p : popups) {
            double age = (now - p.born()) + tp;
            if (age > life) continue;
            float fade = (float) (1.0 - age / life);
            Vec3d pos = p.pos().add(0, age * 0.03, 0);
            double dist = mc.player.getEyePos().distanceTo(pos);
            HighlightRenderer.label(p.text(), pos, HighlightRenderer.withOpacity(color.get(), Math.max(0.15f, fade)),
                HighlightRenderer.labelScale(dist));
        }
    }
}
