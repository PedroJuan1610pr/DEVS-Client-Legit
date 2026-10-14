package com.devs.client.module;

import com.devs.client.profile.ProfileManager;
import com.devs.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Presets rápidos: guardan/cargan perfiles con nombres fijos.
 * Activa "Save X" para guardar el estado actual; "Load X" para aplicarlo.
 */
public final class HudPresets extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting loadPvp = add(new BooleanSetting("Load PvP", false));
    public final BooleanSetting savePvp = add(new BooleanSetting("Save PvP", false));
    public final BooleanSetting loadMining = add(new BooleanSetting("Load Mining", false));
    public final BooleanSetting saveMining = add(new BooleanSetting("Save Mining", false));
    public final BooleanSetting loadStream = add(new BooleanSetting("Load Stream", false));
    public final BooleanSetting saveStream = add(new BooleanSetting("Save Stream", false));

    public HudPresets() {
        super("HUD Presets", "Quick PvP / Mining / Stream config presets.", "Misc");
    }

    @Override public void tick() {
        handle(loadPvp, savePvp, "pvp");
        handle(loadMining, saveMining, "mining");
        handle(loadStream, saveStream, "stream");
    }

    private void handle(BooleanSetting load, BooleanSetting save, String name) {
        if (save.get()) {
            save.set(false);
            boolean ok = ProfileManager.save("preset-" + name);
            msg(ok ? "Saved preset " + name : "Failed to save " + name, ok);
            if (ok) Notifications.push("Preset", "Saved " + name);
        }
        if (load.get()) {
            load.set(false);
            boolean ok = ProfileManager.load("preset-" + name);
            msg(ok ? "Loaded preset " + name : "No preset " + name, ok);
            if (ok) Notifications.push("Preset", "Loaded " + name);
        }
    }

    private void msg(String s, boolean ok) {
        if (mc.player == null) return;
        mc.player.sendMessage(Text.literal("[DEVS] " + s).formatted(ok ? Formatting.GREEN : Formatting.RED));
    }
}
