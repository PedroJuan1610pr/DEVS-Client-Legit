package com.devs.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;

public final class HighlightRenderer {
    private HighlightRenderer() {}
    /** Si es true, los resaltados se ven a través de bloques/paredes. */
    public static boolean throughWalls = true;

    public static void draw(BlockPos pos, HighlightMode mode, int argb, float lineWidth) {
        int color = argb;
        var g = switch (mode) {
            case OUTLINE -> GizmoDrawing.box(pos, DrawStyle.stroked(color, lineWidth));
            case FILLED -> GizmoDrawing.box(pos, DrawStyle.filled(color));
            case OUTLINE_FILLED -> GizmoDrawing.box(pos, DrawStyle.filledAndStroked(color, lineWidth, color));
            case CORNERS -> GizmoDrawing.box(new Box(pos), DrawStyle.stroked(color, lineWidth), true);
        };
        if (throughWalls) g.ignoreOcclusion();
    }

    /** Línea (tracer) entre dos puntos del mundo. */
    public static void line(Vec3d from, Vec3d to, int argb, float lineWidth) {
        var g = GizmoDrawing.line(from, to, argb, lineWidth);
        if (throughWalls) g.ignoreOcclusion();
    }

    /**
     * Texto flotante centrado en el mundo (se ve a través de bloques si throughWalls).
     * OJO: en los mappings Yarn de 1.21.11 los nombres están intercambiados:
     * Style.centered(color) = texto que EMPIEZA en el punto (alineado a la izquierda),
     * Style.left(color)     = texto realmente CENTRADO en el punto.
     */
    public static void label(String text, Vec3d pos, int argb, float scale) {
        var g = GizmoDrawing.text(text, pos, TextGizmo.Style.left(argb).scaled(scale));
        if (throughWalls) g.ignoreOcclusion();
    }

    /** Resaltado de una caja arbitraria (entidades, zonas...). */
    public static void drawBox(Box box, HighlightMode mode, int argb, float lineWidth) {
        var g = switch (mode) {
            case OUTLINE -> GizmoDrawing.box(box, DrawStyle.stroked(argb, lineWidth));
            case FILLED -> GizmoDrawing.box(box, DrawStyle.filled(argb));
            case OUTLINE_FILLED -> GizmoDrawing.box(box, DrawStyle.filledAndStroked(argb, lineWidth, argb));
            case CORNERS -> GizmoDrawing.box(box, DrawStyle.stroked(argb, lineWidth), true);
        };
        if (throughWalls) g.ignoreOcclusion();
    }

    /** Punto de salida de los tracers: justo delante de la cámara (centro de la pantalla). */
    public static Vec3d tracerOrigin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        return mc.gameRenderer.getCamera().getCameraPos().add(mc.player.getRotationVec(tp).multiply(0.6));
    }

    public static HighlightMode parseMode(String name) {
        return switch (name) {
            case "Outline + Filled" -> HighlightMode.OUTLINE_FILLED;
            case "Filled" -> HighlightMode.FILLED;
            case "Corners" -> HighlightMode.CORNERS;
            default -> HighlightMode.OUTLINE;
        };
    }

    public static int withOpacity(int rgb, float alpha) {
        return (rgb & 0x00FFFFFF) | (((int) (Math.max(0, Math.min(1, alpha)) * 255)) << 24);
    }

    /** Escala el texto con la distancia para que siga siendo legible de lejos. */
    public static float labelScale(double dist) { return 0.5f + (float) Math.min(dist, 200) / 200f; }
}
