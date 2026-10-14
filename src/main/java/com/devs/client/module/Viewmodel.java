package com.devs.client.module;

import com.devs.client.setting.BooleanSetting;
import com.devs.client.setting.NumberSetting;

/**
 * Viewmodel (Autism-inspired): mueve/escala/rota el item en primera persona.
 * HeldItemRendererMixin aplica la transformacion.
 */
public final class Viewmodel extends Module {
    public final BooleanSetting mainHand = add(new BooleanSetting("Main Hand", true));
    public final NumberSetting mainScale = add(new NumberSetting("Main Scale", 0.0, -2.0, 2.0, 0.05));
    public final NumberSetting mainX = add(new NumberSetting("Main X", 0.0, -2.0, 2.0, 0.05));
    public final NumberSetting mainY = add(new NumberSetting("Main Y", 0.0, -2.0, 2.0, 0.05));
    public final NumberSetting mainZ = add(new NumberSetting("Main Z", 0.0, -2.0, 2.0, 0.05));
    public final NumberSetting mainRotX = add(new NumberSetting("Main Rot X", 0.0, -90.0, 90.0, 1.0));
    public final NumberSetting mainRotY = add(new NumberSetting("Main Rot Y", 0.0, -90.0, 90.0, 1.0));
    public final NumberSetting mainRotZ = add(new NumberSetting("Main Rot Z", 0.0, -90.0, 90.0, 1.0));

    public final BooleanSetting offHand = add(new BooleanSetting("Off Hand", true));
    public final NumberSetting offScale = add(new NumberSetting("Off Scale", 0.0, -2.0, 2.0, 0.05));
    public final NumberSetting offX = add(new NumberSetting("Off X", 0.0, -1.0, 1.0, 0.02));
    public final NumberSetting offY = add(new NumberSetting("Off Y", 0.0, -1.0, 1.0, 0.02));
    public final NumberSetting offZ = add(new NumberSetting("Off Z", 0.0, -1.0, 1.0, 0.02));
    public final NumberSetting offRotX = add(new NumberSetting("Off Rot X", 0.0, -90.0, 90.0, 1.0));
    public final NumberSetting offRotY = add(new NumberSetting("Off Rot Y", 0.0, -90.0, 90.0, 1.0));
    public final NumberSetting offRotZ = add(new NumberSetting("Off Rot Z", 0.0, -90.0, 90.0, 1.0));

    public Viewmodel() {
        super("Viewmodel", "Customize first-person hand/item position (Autism-inspired).", "Render");
    }

    public boolean active() { return enabled(); }
}
