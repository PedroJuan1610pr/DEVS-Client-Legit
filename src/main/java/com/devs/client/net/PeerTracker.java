package com.devs.client.net;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Jugadores remotos que tienen Devs Client (y su estado de cosméticos). */
public final class PeerTracker {
    public record PeerState(int flags, int color, long updatedAt) {
        public boolean has(int bit) { return (flags & bit) != 0; }
    }

    private static final Map<UUID, PeerState> PEERS = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 15_000;

    private PeerTracker() {}

    public static void put(UUID id, int flags, int color) {
        if (id == null) return;
        PEERS.put(id, new PeerState(flags, color, System.currentTimeMillis()));
    }

    public static boolean has(UUID id) {
        if (id == null) return false;
        PeerState s = PEERS.get(id);
        if (s == null) return false;
        if (System.currentTimeMillis() - s.updatedAt() > TIMEOUT_MS) {
            PEERS.remove(id);
            return false;
        }
        return s.has(DevsPeerPayload.FLAG_ENABLED);
    }

    public static PeerState get(UUID id) {
        if (!has(id)) return null;
        return PEERS.get(id);
    }

    public static void tickCleanup() {
        long now = System.currentTimeMillis();
        PEERS.entrySet().removeIf(e -> now - e.getValue().updatedAt() > TIMEOUT_MS);
    }

    public static void clear() {
        PEERS.clear();
    }

    public static Map<UUID, PeerState> all() {
        return PEERS;
    }
}
