package com.devs.client.multi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Comandos de chat: .multi list|start|stop|chat|add|profiles */
public final class MultiCommands {
    private MultiCommands() {}

    public static boolean handle(String raw) {
        if (raw == null) return false;
        String t = raw.trim();
        if (!t.toLowerCase().startsWith(".multi")) return false;
        String[] p = t.split("\\s+");
        MinecraftClient mc = MinecraftClient.getInstance();
        if (p.length == 1) {
            msg(mc, "Usage: .multi list|start <profile>|stop|chat <msg>|add <name>|profiles", Formatting.YELLOW);
            return true;
        }
        switch (p[1].toLowerCase()) {
            case "list" -> {
                var bots = MultiManager.get().bots();
                if (bots.isEmpty()) msg(mc, "No bots", Formatting.GRAY);
                else for (var b : bots) {
                    msg(mc, b.name() + " — " + b.status() + " " + b.detail(), Formatting.AQUA);
                }
            }
            case "stop" -> MultiManager.get().disconnectAll("Stopped via command");
            case "start" -> {
                if (p.length < 3) { msg(mc, "Usage: .multi start <profile>", Formatting.RED); return true; }
                MultiProfile prof = MultiProfileManager.get().findByName(p[2]);
                if (prof == null) { msg(mc, "Profile not found: " + p[2], Formatting.RED); return true; }
                MultiManager.StartResult r = MultiManager.get().start(prof);
                msg(mc, r.ok() ? "Starting…" : r.message(), r.ok() ? Formatting.GREEN : Formatting.RED);
            }
            case "chat" -> {
                if (p.length < 3) { msg(mc, "Usage: .multi chat <message>", Formatting.RED); return true; }
                String m = t.substring(t.toLowerCase().indexOf("chat") + 4).trim();
                int n = MultiManager.get().broadcastChat(m);
                msg(mc, "Sent to " + n + " bots", Formatting.GREEN);
            }
            case "add" -> {
                if (p.length < 3) { msg(mc, "Usage: .multi add <crackedName>", Formatting.RED); return true; }
                boolean ok = AccountManager.get().add(p[2]);
                msg(mc, ok ? "Account added: " + p[2] : "Failed (duplicate?)", ok ? Formatting.GREEN : Formatting.RED);
            }
            case "profiles" -> {
                var all = MultiProfileManager.get().all();
                if (all.isEmpty()) msg(mc, "No profiles", Formatting.GRAY);
                else for (var pr : all) msg(mc, pr.name + " → " + pr.serverAddress + " (" + pr.accountIds.size() + " acc)", Formatting.AQUA);
            }
            default -> msg(mc, "Unknown: " + p[1], Formatting.RED);
        }
        return true;
    }

    private static void msg(MinecraftClient mc, String s, Formatting f) {
        if (mc.player != null)
            mc.player.sendMessage(Text.literal("[Multi] " + s).formatted(f));
    }
}
