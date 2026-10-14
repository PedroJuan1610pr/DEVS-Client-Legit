package com.devs.client.gui;

/**
 * Paleta de la ClickGUI (colores base idénticos a Autism UiColors).
 * GUI Theme puede retintar el accent/borde.
 */
public final class GuiTheme {
    // Autism UiColors defaults
    public static int SCRIM = 0x66000000;
    public static int WINDOW = 0xD80B0C10;
    public static int WINDOW_STRONG = 0xEE0A0A0D;
    public static int HEADER = 0xF0181A20;
    public static int HEADER_HOVER = 0xFF242832;
    public static int ROW = 0xB012141A;
    public static int ROW_HOVER = 0xC91D222B;
    public static int FIELD = 0xD9121419;
    public static int FIELD_FOCUSED = 0xE81A1E26;
    public static int BORDER = 0xFFCE4949;
    public static int BORDER_SOFT = 0xCC9F3A3A;
    public static int BUTTON_BORDER = 0xE0A85E5E;
    public static int HAIRLINE = 0x2BFFFFFF;
    public static int ACCENT = 0xFFFF3B3B;
    public static int ACCENT_SOFT = 0x44FF3B3B;
    public static int ACCENT_DARK = 0xFF8F1F24;
    public static int TEXT = 0xFFF3ECE7;
    public static int MUTED = 0xFFB79E9E;
    public static int DISABLED = 0xFF766A6A;
    public static int OFF_TRACK = 0xFF3A3A40;
    public static int KNOB = 0xFFE9E9EC;
    public static int BTN_FILL = 0xC01E242D;
    public static int BTN_ACTIVE = 0xCC282E39;

    private GuiTheme() {}

    public static void setAccent(int argb) {
        int rgb = argb & 0x00FFFFFF;
        ACCENT = 0xFF000000 | rgb;
        ACCENT_SOFT = 0x44000000 | rgb;
        ACCENT_DARK = 0xFF000000 | darken(rgb, 0.45f);
        BORDER = 0xFF000000 | darken(rgb, 0.18f);
        BORDER_SOFT = 0xCC000000 | darken(rgb, 0.28f);
        BUTTON_BORDER = 0xE0000000 | darken(rgb, 0.12f);
    }

    public static void setBorder(int argb) {
        int rgb = argb & 0x00FFFFFF;
        BORDER = 0xFF000000 | rgb;
        BORDER_SOFT = 0xCC000000 | rgb;
        BUTTON_BORDER = 0xE0000000 | rgb;
    }

    private static int darken(int rgb, float amount) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        r = Math.max(0, Math.round(r * (1f - amount)));
        g = Math.max(0, Math.round(g * (1f - amount)));
        b = Math.max(0, Math.round(b * (1f - amount)));
        return (r << 16) | (g << 8) | b;
    }
}
