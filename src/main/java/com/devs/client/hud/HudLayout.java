package com.devs.client.hud;

/**
 * Posicion y escala de un elemento del HUD.
 * {@code x}/{@code y} son fracciones de la pantalla (0..1) del punto de anclaje:
 *  - modulos del cliente: esquina superior-izquierda del panel
 *  - scoreboard vanilla: esquina superior-derecha del panel
 */
public final class HudLayout {
    public final String id;
    public final String name;
    public final float defX, defY, defScale;
    public final boolean scoreboard;

    public float x, y, scale;

    /** Ultimo tamano/posicion en pixeles (sin escala) para el editor. */
    public int lastW, lastH;
    public float lastPx, lastPy;

    public HudLayout(String id, String name, float defX, float defY, float defScale, boolean scoreboard) {
        this.id = id;
        this.name = name;
        this.defX = defX;
        this.defY = defY;
        this.defScale = defScale;
        this.scoreboard = scoreboard;
        reset();
    }

    public void reset() {
        x = defX;
        y = defY;
        scale = defScale;
    }

    public void clamp() {
        x = Math.max(0f, Math.min(1f, x));
        y = Math.max(0f, Math.min(1f, y));
        scale = Math.max(0.4f, Math.min(3f, scale));
    }

    /** Rectangulo en pantalla del ultimo render (ya escalado). */
    public int screenX() { return Math.round(lastPx); }
    public int screenY() { return Math.round(lastPy); }
    public int screenW() { return Math.max(8, Math.round(lastW * scale)); }
    public int screenH() { return Math.max(8, Math.round(lastH * scale)); }

    public boolean contains(double mx, double my) {
        int sx = screenX(), sy = screenY();
        int sw = screenW(), sh = screenH();
        return mx >= sx && mx < sx + sw && my >= sy && my < sy + sh;
    }
}
