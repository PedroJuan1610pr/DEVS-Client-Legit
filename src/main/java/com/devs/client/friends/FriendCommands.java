package com.devs.client.friends;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Set;

/**
 * Middle-click sobre un jugador y comandos de chat:
 *   .friend add &lt;nick&gt;
 *   .friend remove &lt;nick&gt;
 *   .friend list
 *   .friend toggle  (el que miras)
 */
public final class FriendCommands {
    private static boolean midDown;

    private FriendCommands() {}

    public static void tick(MinecraftClient mc) {
        if (mc == null || mc.player == null || mc.getWindow() == null) return;
        if (mc.currentScreen != null) {
            midDown = false;
            return;
        }
        long handle = mc.getWindow().getHandle();
        boolean pressed = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (pressed && !midDown) {
            midDown = true;
            onMiddleClick(mc);
        } else if (!pressed) {
            midDown = false;
        }
    }

    private static void onMiddleClick(MinecraftClient mc) {
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            msg(mc, "Mira a un jugador y pulsa la rueda del ratón.", Formatting.GRAY);
            return;
        }
        if (!(hit instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof PlayerEntity target)) {
            msg(mc, "Solo se pueden añadir jugadores.", Formatting.GRAY);
            return;
        }
        if (target == mc.player) {
            msg(mc, "No puedes añadirte a ti mismo.", Formatting.GRAY);
            return;
        }
        boolean nowFriend = FriendsManager.toggle(target);
        String name = target.getName().getString();
        if (nowFriend) {
            msg(mc, "Friend added: " + name, Formatting.GREEN);
        } else {
            msg(mc, "Friend removed: " + name, Formatting.RED);
        }
    }

    /**
     * @return true si el mensaje se consume (no se envía al servidor).
     */
    public static boolean handleChat(String raw) {
        if (raw == null) return false;
        String msg = raw.trim();
        if (!msg.regionMatches(true, 0, ".friend", 0, 7)) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        String[] parts = msg.split("\\s+");
        // .friend | .friend list | .friend add nick | .friend remove nick | .friend toggle
        if (parts.length == 1 || (parts.length == 2 && parts[1].equalsIgnoreCase("list"))) {
            Set<String> names = FriendsManager.names();
            if (names.isEmpty()) {
                msg(mc, "Friends: (vacío)", Formatting.GRAY);
            } else {
                msg(mc, "Friends (" + names.size() + "): " + String.join(", ", names), Formatting.GREEN);
            }
            return true;
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        if (sub.equals("toggle") || sub.equals("add") && parts.length == 2) {
            // toggle looking-at player
            HitResult hit = mc.crosshairTarget;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof PlayerEntity p && p != mc.player) {
                boolean now = FriendsManager.toggle(p);
                msg(mc, (now ? "Friend added: " : "Friend removed: ") + p.getName().getString(),
                    now ? Formatting.GREEN : Formatting.RED);
            } else {
                msg(mc, "Mira a un jugador o usa .friend add <nick>", Formatting.GRAY);
            }
            return true;
        }

        if (parts.length < 3) {
            msg(mc, "Uso: .friend add/remove/list/toggle [nick]", Formatting.YELLOW);
            return true;
        }

        String nick = parts[2];
        if (sub.equals("add")) {
            FriendsManager.addName(nick);
            msg(mc, "Friend added: " + nick, Formatting.GREEN);
            return true;
        }
        if (sub.equals("remove") || sub.equals("del") || sub.equals("rm")) {
            FriendsManager.removeName(nick);
            msg(mc, "Friend removed: " + nick, Formatting.RED);
            return true;
        }

        msg(mc, "Uso: .friend add/remove/list/toggle [nick]", Formatting.YELLOW);
        return true;
    }

    private static void msg(MinecraftClient mc, String text, Formatting color) {
        if (mc == null || mc.player == null) return;
        mc.player.sendMessage(Text.literal("[DEVS] " + text).formatted(color));
    }
}
