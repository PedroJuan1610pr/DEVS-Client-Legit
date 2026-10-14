package com.devs.client.module;

import com.devs.client.gui.MultiScreen;
import com.devs.client.multi.MultiManager;
import net.minecraft.client.MinecraftClient;

/** Abre el panel Multi (bots cracked). Keybind = abrir GUI. */
public final class MultiModule extends Module {
    public MultiModule() {
        super("Multi", "Multi-account (cracked bots). Bind a key to open the panel.", "Misc");
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> mc.setScreen(new MultiScreen(null)));
        // no dejar el módulo "on" permanentemente — es un atajo a la GUI
        setEnabled(false);
    }

    @Override
    public void tick() {
        // opcional: nada
    }
}
