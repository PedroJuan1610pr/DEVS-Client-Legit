package com.devs.client.module;

import com.devs.client.setting.BooleanSetting;

/** Oculta la lluvia/nieve y las tormentas solo en tu cliente. La lógica está en WeatherMixin. */
public final class WeatherControl extends Module {
    public final BooleanSetting noRain = add(new BooleanSetting("No Rain / Snow", true));
    public final BooleanSetting noThunder = add(new BooleanSetting("No Thunder", true));

    public WeatherControl() {
        super("Weather Control", "Client-side only: hides rain, snow and thunder.", "Render");
    }
}
