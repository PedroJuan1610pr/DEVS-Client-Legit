package com.devs.client.module;

import com.devs.client.setting.NumberSetting;

public final class Fullbright extends Module {
    /** Gamma que se aplica mientras el módulo está activo (vanilla llega a 1.0). */
    public final NumberSetting gamma = add(new NumberSetting("Gamma", 12.0, 2.0, 16.0, 1.0));

    public Fullbright() {
        super("Fullbright", "Removes darkness: full brightness everywhere.", "Render");
    }
}
