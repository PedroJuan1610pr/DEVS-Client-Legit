package com.devs.client.module;

import com.devs.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String name, description, category;
    private boolean enabled;
    /** GLFW key code, -1 = unbound. */
    private int keybind = -1;
    private final List<Setting<?>> settings = new ArrayList<>();

    protected Module(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** Enable/disable without depending on current state. */
    public void setEnabled(boolean on) {
        if (enabled == on) return;
        enabled = on;
        if (enabled) onEnable();
        else onDisable();
        if (!(this instanceof Notifications) && !(this instanceof Panic)) {
            try {
                if (Notifications.moduleToggleEnabled())
                    Notifications.push(name, enabled ? "enabled" : "disabled",
                        enabled ? 0xFF6FE3A1 : 0xFFFF4D6D);
            } catch (Throwable ignored) {}
        }
    }

    public boolean enabled() { return enabled; }
    public String name() { return name; }
    public String description() { return description; }
    public String category() { return category; }
    public List<Setting<?>> settings() { return settings; }

    public int keybind() { return keybind; }
    public void setKeybind(int key) { this.keybind = key; }

    protected void onEnable() {}
    protected void onDisable() {}

    public void tick() {}
    public void render() {}
    public void renderHud(DrawContext ctx) {}
}
