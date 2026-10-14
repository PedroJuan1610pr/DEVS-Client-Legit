package com.devs.client.module;

import com.devs.client.gui.GuiTheme;
import com.devs.client.setting.ColorSetting;

/**
 * Personaliza el color de acento/borde de la ClickGUI.
 * Los colores se aplican siempre (no hace falta activar el modulo).
 */
public final class GuiThemeModule extends Module {
    public final ColorSetting accent = add(new ColorSetting("Accent", 0xFFFF3B3B));
    public final ColorSetting border = add(new ColorSetting("Border", 0xFFCE4949));

    public GuiThemeModule() {
        super("GUI Theme", "Customize ClickGUI accent and border colors.", "Misc");
    }

    @Override public void tick() { apply(); }

    public void apply() {
        GuiTheme.setAccent(accent.get());
        // If user set a custom border, use it; otherwise setAccent already derived borders.
        // Re-apply border explicitly so user control works.
        GuiTheme.setBorder(border.get());
        // Keep accent soft derived from accent after border overwrite
        int rgb = accent.get() & 0x00FFFFFF;
        GuiTheme.ACCENT = 0xFF000000 | rgb;
        GuiTheme.ACCENT_SOFT = 0x44000000 | rgb;
    }

    @Override protected void onEnable() { apply(); }
}
