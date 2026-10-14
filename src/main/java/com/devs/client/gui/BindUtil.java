package com.devs.client.gui;

import org.lwjgl.glfw.GLFW;

/** Nombres cortos de teclas (estilo Autism). */
public final class BindUtil {
    private BindUtil() {}

    public static String name(int key) {
        if (key < 0) return "None";
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "Alt";
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> "Super";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "Back";
            case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_KEY_INSERT -> "Ins";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_F1 -> "F1"; case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3"; case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5"; case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7"; case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9"; case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11"; case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_KP_0 -> "Num0"; case GLFW.GLFW_KEY_KP_1 -> "Num1";
            case GLFW.GLFW_KEY_KP_2 -> "Num2"; case GLFW.GLFW_KEY_KP_3 -> "Num3";
            case GLFW.GLFW_KEY_KP_4 -> "Num4"; case GLFW.GLFW_KEY_KP_5 -> "Num5";
            case GLFW.GLFW_KEY_KP_6 -> "Num6"; case GLFW.GLFW_KEY_KP_7 -> "Num7";
            case GLFW.GLFW_KEY_KP_8 -> "Num8"; case GLFW.GLFW_KEY_KP_9 -> "Num9";
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "M1";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "M2";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "M3";
            case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
            case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
            default -> {
                String n = GLFW.glfwGetKeyName(key, 0);
                if (n != null && !n.isBlank()) yield n.toUpperCase();
                yield "K" + key;
            }
        };
    }

    public static int clearOr(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) return -1;
        return key;
    }
}
