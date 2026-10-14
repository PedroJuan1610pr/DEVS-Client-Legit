package com.devs.client.net;

import com.devs.client.DevsClient;
import com.devs.client.module.Cosmetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sync estilo Cosmetica via HTTP.
 * Orden de resolución (sin que el usuario instale nada):
 * 1) config/devs/peer-api.txt (si hay URL válida)
 * 2) Gist de GitHub con la URL Cloudflare actual (como proyecto Licencias)
 * 3) UDP en LAN (puerto 8788)
 * 4) Tailscale / LAN / localhost
 * Cuando encuentra una URL viva, la guarda en peer-api.txt.
 */
public final class PeerApiClient {
    private static final int HTTP_PORT = 8787;
    private static final int UDP_PORT = 8788;
    private static final byte[] DISCOVER_REQ = "DEVS_PEER_DISCOVER".getBytes(StandardCharsets.UTF_8);
    private static final String DISCOVER_PREFIX = "DEVS_PEER_API ";

    /**
     * Gist fijo (mismo sistema que el proyecto Licencias).
     * El Ubuntu publica aquí la URL trycloudflare actual → el cliente la lee solo.
     */
    private static final String GIST_DISCOVERY =
        "https://gist.githubusercontent.com/PedroJuan1610pr/6cff64c9557c02e22ab743c7a912ff96/raw/peer-api.json";

    /** Fallbacks: Tailscale + LAN típica + local. */
    private static final String[] DEFAULT_CANDIDATES = {
        "http://100.74.58.21:8787",
        "http://192.168.1.192:8787",
        "http://127.0.0.1:8787",
    };

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "devs-peer-api");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);
    private static final AtomicBoolean RESOLVING = new AtomicBoolean(false);

    private static volatile String baseUrl = "";
    private static long lastLoadCfg;
    private static long lastDiscoverAttempt;
    private static int tick;

    private PeerApiClient() {}

    public static void tick() {
        tick++;
        if (tick % 40 != 0) return;
        if (BUSY.get()) return;

        String url = baseUrl();
        if (url.isEmpty()) {
            // Intentar descubrir en background (como máximo cada 15 s)
            long now = System.currentTimeMillis();
            if (now - lastDiscoverAttempt > 15_000 && RESOLVING.compareAndSet(false, true)) {
                lastDiscoverAttempt = now;
                IO.execute(() -> {
                    try {
                        String found = autoResolve();
                        if (found != null && !found.isEmpty()) {
                            baseUrl = found;
                            saveConfig(found);
                        }
                    } finally {
                        RESOLVING.set(false);
                    }
                });
            }
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        BUSY.set(true);
        final String use = url;
        IO.execute(() -> {
            try {
                postPresence(use, mc);
                fetchPeers(use, mc);
            } catch (Throwable ignored) {
            } finally {
                BUSY.set(false);
            }
        });
    }

    public static String baseUrl() {
        long now = System.currentTimeMillis();
        if (now - lastLoadCfg > 5000) {
            lastLoadCfg = now;
            String fromFile = readConfig();
            if (!fromFile.isEmpty()) {
                baseUrl = fromFile;
            }
        }
        return baseUrl == null ? "" : baseUrl;
    }

    /** Prueba config → Gist (Cloudflare) → UDP LAN → defaults. */
    private static String autoResolve() {
        // 1) Config manual
        String cfg = readConfig();
        if (!cfg.isEmpty() && isHealthy(cfg)) return cfg;

        // 2) Gist público con la URL trycloudflare actual (como Licencias)
        String gist = fetchGistUrl();
        if (gist != null && !gist.isEmpty() && isHealthy(gist)) return gist;

        // 3) UDP en LAN
        String udp = discoverUdp(1500);
        if (udp != null && isHealthy(udp)) return udp;

        // 4) Candidatos fijos
        for (String c : DEFAULT_CANDIDATES) {
            if (isHealthy(c)) return c;
        }
        return "";
    }

    /** Lee {"url":"https://....trycloudflare.com"} del Gist. */
    private static String fetchGistUrl() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GIST_DISCOVERY + "?t=" + System.currentTimeMillis()))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "DevsClient/2.0")
                .header("Cache-Control", "no-cache")
                .GET()
                .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2 || res.body() == null) return null;
            JsonObject o = JsonParser.parseString(res.body()).getAsJsonObject();
            if (!o.has("url")) return null;
            String url = o.get("url").getAsString().trim();
            while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            if (url.startsWith("http://") || url.startsWith("https://")) return url;
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean isHealthy(String base) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/health"))
                .timeout(Duration.ofSeconds(2))
                .header("User-Agent", "DevsClient/2.0")
                .GET()
                .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return res.statusCode() / 100 == 2 && res.body() != null && res.body().contains("\"ok\"");
        } catch (Throwable t) {
            return false;
        }
    }

    private static String discoverUdp(int timeoutMs) {
        try (DatagramSocket sock = new DatagramSocket()) {
            sock.setBroadcast(true);
            sock.setSoTimeout(timeoutMs);

            Set<InetAddress> targets = new LinkedHashSet<>();
            targets.add(InetAddress.getByName("255.255.255.255"));
            try {
                // Broadcasts por interfaz
                var ifaces = java.net.NetworkInterface.getNetworkInterfaces();
                while (ifaces.hasMoreElements()) {
                    var ni = ifaces.nextElement();
                    if (!ni.isUp() || ni.isLoopback()) continue;
                    for (var ia : ni.getInterfaceAddresses()) {
                        InetAddress b = ia.getBroadcast();
                        if (b != null) targets.add(b);
                    }
                }
            } catch (Throwable ignored) {}

            byte[] buf = new byte[512];
            for (InetAddress dest : targets) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                        DISCOVER_REQ, DISCOVER_REQ.length, dest, UDP_PORT);
                    sock.send(packet);
                } catch (Throwable ignored) {}
            }

            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    DatagramPacket resp = new DatagramPacket(buf, buf.length);
                    sock.receive(resp);
                    String msg = new String(resp.getData(), 0, resp.getLength(), StandardCharsets.UTF_8).trim();
                    if (msg.startsWith(DISCOVER_PREFIX)) {
                        String url = msg.substring(DISCOVER_PREFIX.length()).trim();
                        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            return url;
                        }
                    }
                } catch (java.net.SocketTimeoutException e) {
                    break;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String readConfig() {
        try {
            Path f = configPath();
            if (!Files.isRegularFile(f)) {
                Files.createDirectories(f.getParent());
                Files.writeString(f,
                    "# Devs Peer API — déjalo vacío para auto-descubrimiento en LAN\n"
                        + "# O pon una URL fija, ej:\n"
                        + "# http://192.168.1.50:8787\n"
                        + "# http://100.74.58.21:8787\n");
                return "";
            }
            for (String line : Files.readAllLines(f)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                while (line.endsWith("/")) line = line.substring(0, line.length() - 1);
                return line;
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private static void saveConfig(String url) {
        try {
            Path f = configPath();
            Files.createDirectories(f.getParent());
            Files.writeString(f,
                "# Auto-detectado por Devs Client — puedes cambiarlo a mano\n"
                    + url + "\n");
        } catch (Throwable ignored) {}
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("peer-api.txt");
    }

    private static void postPresence(String base, MinecraftClient mc) throws Exception {
        UUID id = mc.player.getUuid();
        String name = mc.player.getName().getString();
        int flags = DevsPeerPayload.FLAG_ENABLED;
        int color = 0xFF4DD2FF;
        Cosmetics c = findCosmetics();
        if (c != null && c.enabled()) {
            if (c.chinaHat.get()) flags |= DevsPeerPayload.FLAG_CHINA_HAT;
            if (c.trail.get()) flags |= DevsPeerPayload.FLAG_TRAIL;
            if (c.halo.get()) flags |= DevsPeerPayload.FLAG_HALO;
            if (c.rings.get()) flags |= DevsPeerPayload.FLAG_RINGS;
            if (c.orbs.get()) flags |= DevsPeerPayload.FLAG_ORBS;
            if (c.mahoraga.get()) flags |= DevsPeerPayload.FLAG_MAHORAGA;
            color = c.color.get() | 0xFF000000;
        }

        JsonObject json = new JsonObject();
        json.addProperty("uuid", id.toString());
        json.addProperty("name", name);
        json.addProperty("flags", flags);
        json.addProperty("color", color);
        json.addProperty("server", serverHint(mc));

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(base + "/api/v1/presence"))
            .timeout(Duration.ofSeconds(4))
            .header("Content-Type", "application/json")
            .header("User-Agent", "DevsClient/2.0")
            .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
            .build();
        HTTP.send(req, HttpResponse.BodyHandlers.discarding());
    }

    private static void fetchPeers(String base, MinecraftClient mc) throws Exception {
        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        if (nh == null) return;
        List<String> ids = new ArrayList<>();
        for (PlayerListEntry e : nh.getPlayerList()) {
            if (e.getProfile() == null) continue;
            UUID u = e.getProfile().id();
            if (u.equals(mc.player.getUuid())) continue;
            ids.add(u.toString());
        }
        if (ids.isEmpty()) return;

        String q = String.join(",", ids);
        // Solo filtramos por UUIDs del tab: ya están en el mismo server MC.
        // El filtro ?server= rompía en servers públicos (IP vs dominio, con/sin puerto).
        StringBuilder uri = new StringBuilder(base).append("/api/v1/players?uuids=")
            .append(URLEncoder.encode(q, StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(uri.toString()))
            .timeout(Duration.ofSeconds(4))
            .header("User-Agent", "DevsClient/2.0")
            .GET()
            .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() / 100 != 2) return;

        JsonElement root = JsonParser.parseString(res.body());
        if (!root.isJsonArray()) return;
        JsonArray arr = root.getAsJsonArray();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (!o.has("uuid")) continue;
            UUID u = UUID.fromString(o.get("uuid").getAsString());
            int flags = o.has("flags") ? o.get("flags").getAsInt() : 1;
            int color = o.has("color") ? o.get("color").getAsInt() : 0xFF4DD2FF;
            PeerTracker.put(u, flags, color);
        }
    }

    private static Cosmetics findCosmetics() {
        for (var m : DevsClient.MODULES.all()) {
            if (m instanceof Cosmetics cos) return cos;
        }
        return null;
    }

    private static String serverHint(MinecraftClient mc) {
        try {
            if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
                String a = mc.getCurrentServerEntry().address.trim().toLowerCase();
                // quitar puerto por defecto de MC
                if (a.endsWith(":25565")) a = a.substring(0, a.length() - 6);
                return a;
            }
            if (mc.isInSingleplayer()) return "singleplayer";
        } catch (Throwable ignored) {}
        return "";
    }
}
