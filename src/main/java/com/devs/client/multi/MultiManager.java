package com.devs.client.multi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orquestador Multi (estilo Autism): perfiles, bots cracked, start/stop, chat broadcast.
 */
public final class MultiManager implements MultiSession.Sink {
    public record StartResult(boolean ok, String message) {
        public static StartResult success() { return new StartResult(true, ""); }
        public static StartResult err(String m) { return new StartResult(false, m); }
    }

    public record BotView(String accountId, String name, MultiSession.Status status, String detail,
                          boolean ready, double x, double y, double z, boolean hasPos) {}

    private static MultiManager INSTANCE;
    private final AtomicLong generations = new AtomicLong();
    private final ExecutorService workers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "devs-multi");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, MultiSession> sessions = new LinkedHashMap<>();
    private final List<String> systemLog = new CopyOnWriteArrayList<>();
    private volatile boolean active;
    private volatile long generation;
    private volatile MultiProfile activeProfile;

    public static MultiManager get() {
        if (INSTANCE == null) INSTANCE = new MultiManager();
        return INSTANCE;
    }

    public boolean isActive() { return active; }
    public MultiProfile activeProfile() { return activeProfile; }

    public synchronized StartResult start(MultiProfile source) {
        if (source == null) return StartResult.err("Profile missing");
        if (active) return StartResult.err("Stop the active Multi batch first");
        MultiProfile profile = new MultiProfile(source);
        profile.normalize();
        if (profile.accountIds.isEmpty()) return StartResult.err("Add at least one account to the profile");
        if (profile.serverAddress.isBlank()) return StartResult.err("Set server address");

        // no conectar la cuenta actual del juego si coincide
        MinecraftClient mc = MinecraftClient.getInstance();
        String selfName = mc.player != null ? mc.player.getName().getString() : "";

        List<DevsAccount> toConnect = new ArrayList<>();
        for (String id : profile.accountIds) {
            DevsAccount a = AccountManager.get().findById(id);
            if (a == null) return StartResult.err("Unknown account id: " + id);
            if (!selfName.isBlank() && selfName.equalsIgnoreCase(a.displayName())) {
                continue; // skip self
            }
            toConnect.add(a);
        }
        if (toConnect.isEmpty()) return StartResult.err("No accounts left to connect (maybe you're already on one)");

        generation = generations.incrementAndGet();
        activeProfile = profile;
        active = true;
        sessions.clear();
        systemLog.clear();
        onSystem("Starting Multi on " + profile.serverAddress + " (" + toConnect.size() + " bots)");

        int delay = profile.pacing.delayMs;
        int i = 0;
        for (DevsAccount a : toConnect) {
            MultiSession s = new MultiSession(generation, a, profile.serverAddress, this);
            sessions.put(a.id, s);
            final int idx = i++;
            workers.execute(() -> {
                try {
                    if (delay > 0 && idx > 0) Thread.sleep((long) delay * idx);
                } catch (InterruptedException ignored) {}
                if (!active || generation != s.generation()) return;
                s.start(workers);
            });
        }
        return StartResult.success();
    }

    public synchronized void disconnectAll(String reason) {
        active = false;
        for (MultiSession s : new ArrayList<>(sessions.values())) {
            s.disconnect(reason == null ? "Stopped" : reason);
        }
        sessions.clear();
        activeProfile = null;
        onSystem("Multi stopped");
    }

    public synchronized void disconnect(String accountId) {
        MultiSession s = sessions.remove(accountId);
        if (s != null) s.disconnect("Removed");
        if (sessions.isEmpty()) {
            active = false;
            activeProfile = null;
        }
    }

    public List<BotView> bots() {
        List<BotView> out = new ArrayList<>();
        for (MultiSession s : sessions.values()) {
            out.add(new BotView(
                s.accountId(), s.name(), s.status(), s.detail(),
                s.ready(), s.x(), s.y(), s.z(), s.hasPosition()
            ));
        }
        return out;
    }

    public MultiSession session(String accountId) {
        return sessions.get(accountId);
    }

    public int broadcastChat(String message) {
        int n = 0;
        for (MultiSession s : sessions.values()) {
            if (s.ready()) {
                s.sendChat(message);
                n++;
            }
        }
        return n;
    }

    public int broadcastCommand(String command) {
        int n = 0;
        for (MultiSession s : sessions.values()) {
            if (s.ready()) {
                s.sendCommand(command);
                n++;
            }
        }
        return n;
    }

    public List<String> systemLog() {
        return List.copyOf(systemLog);
    }

    @Override
    public void onStatus(MultiSession session) {
        // no-op; GUI poll bots()
    }

    @Override
    public void onChat(MultiSession session, String message) {
        onSystem("[" + session.name() + "] " + message);
    }

    @Override
    public void onSystem(String message) {
        if (message == null) return;
        systemLog.add(message);
        while (systemLog.size() > 200) systemLog.remove(0);
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.execute(() -> {
                    if (mc.player != null)
                        mc.player.sendMessage(Text.literal("[Multi] " + message).formatted(Formatting.GRAY));
                });
            }
        } catch (Throwable ignored) {}
    }
}
