package com.devs.client.module;

import com.devs.client.setting.BooleanSetting;

/**
 * NoRender (Autism-inspired). Los mixins leen estos flags.
 * Solo se implementan los hooks estables en 1.21.11 (overlays, HUD, weather, hurtcam, totem).
 */
public final class NoRender extends Module {
    // Overlay
    public final BooleanSetting fire = add(new BooleanSetting("Fire Overlay", true));
    public final BooleanSetting liquid = add(new BooleanSetting("Underwater Overlay", false));
    public final BooleanSetting inWall = add(new BooleanSetting("In-Wall Overlay", false));
    public final BooleanSetting nausea = add(new BooleanSetting("Nausea", true));
    public final BooleanSetting totem = add(new BooleanSetting("Totem Animation", false));
    public final BooleanSetting hurtCam = add(new BooleanSetting("Hurt Camera", true));

    // HUD
    public final BooleanSetting bossBar = add(new BooleanSetting("Boss Bar", false));
    public final BooleanSetting scoreboard = add(new BooleanSetting("Scoreboard", false));
    public final BooleanSetting crosshair = add(new BooleanSetting("Crosshair", false));
    public final BooleanSetting title = add(new BooleanSetting("Title / Subtitle", false));
    public final BooleanSetting potionIcons = add(new BooleanSetting("Potion Icons", false));
    public final BooleanSetting heldItemName = add(new BooleanSetting("Held Item Name", false));

    // World
    public final BooleanSetting weather = add(new BooleanSetting("Weather", false));
    public final BooleanSetting fog = add(new BooleanSetting("Fog", false));
    public final BooleanSetting blindness = add(new BooleanSetting("Blindness Fog", false));

    public NoRender() { super("NoRender", "Hides selected overlays, HUD and world noise (Autism-inspired).", "Render"); }

    public boolean hideFire() { return enabled() && fire.get(); }
    public boolean hideLiquid() { return enabled() && liquid.get(); }
    public boolean hideInWall() { return enabled() && inWall.get(); }
    public boolean hideNausea() { return enabled() && nausea.get(); }
    public boolean hideTotem() { return enabled() && totem.get(); }
    public boolean hideHurtCam() { return enabled() && hurtCam.get(); }
    public boolean hideBossBar() { return enabled() && bossBar.get(); }
    public boolean hideScoreboard() { return enabled() && scoreboard.get(); }
    public boolean hideCrosshair() { return enabled() && crosshair.get(); }
    public boolean hideTitle() { return enabled() && title.get(); }
    public boolean hidePotionIcons() { return enabled() && potionIcons.get(); }
    public boolean hideHeldItemName() { return enabled() && heldItemName.get(); }
    public boolean hideWeather() { return enabled() && weather.get(); }
    public boolean hideFog() { return enabled() && fog.get(); }
    public boolean hideBlindness() { return enabled() && blindness.get(); }
}
