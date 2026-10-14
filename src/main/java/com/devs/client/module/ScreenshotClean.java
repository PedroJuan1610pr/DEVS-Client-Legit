package com.devs.client.module;

import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;

/**
 * Cuando está activo, el HUD custom de DEVS se oculta si el HUD vanilla está oculto (F1)
 * o si activas "Hide On F1". Sirve para capturas más limpias.
 */
public final class ScreenshotClean extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting hideOnF1 = add(new BooleanSetting("Hide On F1", true));
    public final BooleanSetting forceHide = add(new BooleanSetting("Force Hide DEVS HUD", false));

    public static ScreenshotClean INSTANCE;

    public ScreenshotClean() {
        super("Screenshot Clean", "Hides DEVS HUD for cleaner screenshots (F1 / force).", "Misc");
        INSTANCE = this;
    }

    public static boolean shouldHideHud() {
        if (INSTANCE == null || !INSTANCE.enabled()) return false;
        if (INSTANCE.forceHide.get()) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        return INSTANCE.hideOnF1.get() && mc.options.hudHidden;
    }
}
