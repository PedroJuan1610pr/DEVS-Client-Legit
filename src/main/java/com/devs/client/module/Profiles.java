package com.devs.client.module;

import com.devs.client.profile.ProfileManager;
import com.devs.client.setting.BooleanSetting;
import com.devs.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Guarda/carga la configuración de todos los módulos en 5 ranuras.
 * "Save" y "Load" se comportan como botones: se pulsan y vuelven solos a OFF.
 * Además, al cerrar el juego se guarda "auto" y al iniciarlo se carga (ver DevsClient).
 */
public final class Profiles extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting slot = add(new ModeSetting("Slot", "Slot 1", "Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5"));
    public final BooleanSetting save = add(new BooleanSetting("Save (click)", false));
    public final BooleanSetting load = add(new BooleanSetting("Load (click)", false));
    public final BooleanSetting autoSave = add(new BooleanSetting("Auto Save On Exit", true));

    public Profiles() { super("Profiles", "Save and load all module settings (5 slots + auto).", "Misc"); }

    /** tick() se llama siempre, así que los botones funcionan aunque el módulo esté apagado. */
    @Override public void tick() {
        if (save.get()) {
            save.set(false);
            say(ProfileManager.save(slot.get()) ? "Saved " + slot.get() : "Could not save " + slot.get());
        }
        if (load.get()) {
            load.set(false);
            say(ProfileManager.load(slot.get()) ? "Loaded " + slot.get() : slot.get() + " is empty or unreadable");
        }
    }

    private void say(String msg) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[DEVS] " + msg).formatted(Formatting.GREEN));
    }
}
