package com.devs.client.module;

import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.BooleanSetting;
import com.devs.client.setting.ColorSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Marcador de última muerte. Solo si el módulo está activo. */
public final class DeathPoint extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting chat = add(new BooleanSetting("Chat Message", true));
    public final BooleanSetting label = add(new BooleanSetting("Label", true));
    public final BooleanSetting tracer = add(new BooleanSetting("Tracer", true));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFF4D6D));

    private Vec3d deathPos;
    private Identifier deathDim;
    private boolean wasDead;

    public DeathPoint() {
        super("Death Point", "Marks where you died (box + tracer). Disable if you use Waypoints On Death.", "Misc");
    }

    @Override
    public void onDisable() {
        deathPos = null;
        deathDim = null;
        wasDead = false;
    }

    @Override
    public void tick() {
        if (!enabled()) {
            wasDead = false;
            return;
        }
        var p = mc.player;
        if (p == null || mc.world == null) {
            wasDead = false;
            return;
        }
        boolean dead = p.isDead() || p.getHealth() <= 0;
        if (dead && !wasDead) {
            deathPos = p.getEntityPos();
            deathDim = mc.world.getRegistryKey().getValue();
            if (chat.get()) {
                p.sendMessage(Text.literal(String.format("[DEVS] You died at %d, %d, %d (%s)",
                    Math.round(deathPos.x), Math.round(deathPos.y), Math.round(deathPos.z), deathDim.getPath()))
                    .formatted(Formatting.RED));
            }
        }
        wasDead = dead;
    }

    @Override
    public void render() {
        if (!enabled()) return;
        if (deathPos == null || mc.world == null || mc.player == null) return;
        if (!mc.world.getRegistryKey().getValue().equals(deathDim)) return;
        int c = color.get();
        double dist = mc.player.getEyePos().distanceTo(deathPos);
        HighlightRenderer.drawBox(Box.of(deathPos.add(0, 0.5, 0), 0.8, 1.0, 0.8), HighlightMode.OUTLINE_FILLED,
            HighlightRenderer.withOpacity(c, 0.5f), 2f);
        if (tracer.get()) {
            HighlightRenderer.line(HighlightRenderer.tracerOrigin(), deathPos.add(0, 0.5, 0), c, 2f);
        }
        if (label.get()) {
            HighlightRenderer.label("Death [" + Math.round(dist) + "m]", deathPos.add(0, 1.6, 0),
                c | 0xFF000000, HighlightRenderer.labelScale(dist));
        }
    }
}
