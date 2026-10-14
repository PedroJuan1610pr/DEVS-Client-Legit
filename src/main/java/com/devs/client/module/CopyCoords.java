package com.devs.client.module;

import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Copia coordenadas al portapapeles (bind key o botón). */
public final class CopyCoords extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting includeDim = add(new BooleanSetting("Include Dimension", true));
    public final BooleanSetting deathAuto = add(new BooleanSetting("Copy On Death", true));

    private boolean wasDead;

    public CopyCoords() { super("Copy Coords", "Copy XYZ to clipboard (bind a key). Auto on death optional.", "Misc"); }

    @Override protected void onEnable() {
        doCopy();
        setEnabled(false); // one-shot
    }

    @Override public void tick() {
        if (!deathAuto.get() || mc.player == null) { wasDead = false; return; }
        boolean dead = mc.player.isDead() || mc.player.getHealth() <= 0;
        if (dead && !wasDead) doCopy();
        wasDead = dead;
    }

    private void doCopy() {
        if (mc.player == null) return;
        String s = String.format("%d %d %d", Math.round(mc.player.getX()), Math.round(mc.player.getY()), Math.round(mc.player.getZ()));
        if (includeDim.get() && mc.world != null)
            s += " [" + mc.world.getRegistryKey().getValue().getPath() + "]";
        mc.keyboard.setClipboard(s);
        if (mc.player != null)
            mc.player.sendMessage(Text.literal("[DEVS] Copied: " + s).formatted(Formatting.AQUA));
        Notifications.push("Coords", s, 0xFF4DD2FF);
    }
}
