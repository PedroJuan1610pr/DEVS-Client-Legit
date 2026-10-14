package com.devs.client.setting;

/** Texto libre (una o varias lineas). Usa \n para saltos de linea. */
public class StringSetting extends Setting<String> {
    private final int maxLen;

    public StringSetting(String name, String value) {
        this(name, value, 256);
    }

    public StringSetting(String name, String value, int maxLen) {
        super(name, value == null ? "" : value);
        this.maxLen = Math.max(1, maxLen);
    }

    public int maxLen() { return maxLen; }

    @Override public void set(String value) {
        if (value == null) value = "";
        if (value.length() > maxLen) value = value.substring(0, maxLen);
        super.set(value);
    }
}
