package com.devs.client.setting;
public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean value) { super(name, value); }
    public void toggle() { set(!get()); }
}
