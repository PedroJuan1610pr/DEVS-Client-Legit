package com.devs.client.net;

import com.devs.client.DevsClient;
import com.devs.client.module.Cosmetics;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

public final class DevsNetworking {
    private static boolean registered;
    private static int tick;

    private DevsNetworking() {}

    /** Llamar una vez desde main (servidor + cliente). */
    public static void registerCommon() {
        if (registered) return;
        registered = true;
        // Fabric API 26.1+: playC2S/playS2C → serverboundPlay/clientboundPlay
        PayloadTypeRegistry.serverboundPlay().register(DevsPeerPayload.ID, DevsPeerPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DevsPeerPayload.ID, DevsPeerPayload.CODEC);

        // Servidor: reenvía el estado de un jugador a todos los demás
        ServerPlayNetworking.registerGlobalReceiver(DevsPeerPayload.ID, (payload, context) -> {
            ServerPlayerEntity sender = context.player();
            if (sender == null) return;
            MinecraftServer server = resolveServer(sender);
            if (server == null) return;
            UUID id = sender.getUuid();
            DevsPeerPayload out = new DevsPeerPayload(id, payload.flags(), payload.color());
            for (ServerPlayerEntity p : listPlayers(server)) {
                if (p.getUuid().equals(id)) continue;
                ServerPlayNetworking.send(p, out);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // nada: el timeout del PeerTracker limpia
        });
    }

    /**
     * Compat 26.1.2 (Modern Yarn / Mojmap): getServer() / getServerWorld() ya no existen.
     * Resolvemos por reflexión: field "server" o level()/getWorld().getServer().
     */
    private static MinecraftServer resolveServer(ServerPlayerEntity sender) {
        // 1) field MinecraftServer server (private en ServerPlayer)
        for (String name : new String[]{"server", "field_13995"}) {
            try {
                Field f = findField(sender.getClass(), name);
                if (f != null) {
                    f.setAccessible(true);
                    Object v = f.get(sender);
                    if (v instanceof MinecraftServer ms) return ms;
                }
            } catch (Throwable ignored) {}
        }
        // 2) level() / getWorld() / getEntityWorld() → getServer()
        for (String worldMethod : new String[]{"level", "getWorld", "getEntityWorld", "getServerWorld", "method_51469"}) {
            try {
                Method m = findMethod(sender.getClass(), worldMethod);
                if (m == null) continue;
                m.setAccessible(true);
                Object world = m.invoke(sender);
                if (world == null) continue;
                Method gs = findMethod(world.getClass(), "getServer");
                if (gs == null) gs = findMethod(world.getClass(), "getMinecraftServer");
                if (gs == null) continue;
                gs.setAccessible(true);
                Object s = gs.invoke(world);
                if (s instanceof MinecraftServer ms) return ms;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Collection<ServerPlayerEntity> listPlayers(MinecraftServer server) {
        // yarn: getPlayerManager().getPlayerList()
        // mojmap: getPlayerList().getPlayers()
        try {
            Method gpm = findMethod(server.getClass(), "getPlayerManager");
            if (gpm != null) {
                Object pm = gpm.invoke(server);
                Method gpl = findMethod(pm.getClass(), "getPlayerList");
                if (gpl != null) {
                    Object list = gpl.invoke(pm);
                    if (list instanceof Collection<?> c) return (Collection<ServerPlayerEntity>) c;
                }
            }
        } catch (Throwable ignored) {}
        try {
            Method gpl = findMethod(server.getClass(), "getPlayerList");
            if (gpl != null) {
                Object pl = gpl.invoke(server);
                Method gp = findMethod(pl.getClass(), "getPlayers");
                if (gp == null) gp = findMethod(pl.getClass(), "getPlayerList");
                if (gp != null) {
                    Object list = gp.invoke(pl);
                    if (list instanceof Collection<?> c) return (Collection<ServerPlayerEntity>) c;
                }
            }
        } catch (Throwable ignored) {}
        return java.util.List.of();
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
            }
        }
        return null;
    }

    /** Llamar desde el cliente. */
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(DevsPeerPayload.ID, (payload, context) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && payload.player().equals(mc.player.getUuid())) return;
            PeerTracker.put(payload.player(), payload.flags(), payload.color());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PeerTracker.clear());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            tick = 0;
            sendLocalState(true);
        });
    }

    public static void clientTick() {
        tick++;
        PeerTracker.tickCleanup();
        // Fabric packet (solo si el server de MC tiene el mod)
        if (tick % 40 == 0) sendLocalState(false);
        // HTTP API casera (cualquier server de MC) — estilo Cosmetica
        PeerApiClient.tick();
    }

    public static void sendLocalState(boolean force) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!ClientPlayNetworking.canSend(DevsPeerPayload.ID)) return;

        Cosmetics c = null;
        for (var m : DevsClient.MODULES.all()) {
            if (m instanceof Cosmetics cos) { c = cos; break; }
        }
        int flags = DevsPeerPayload.FLAG_ENABLED;
        int color = 0xFF4DD2FF;
        if (c != null && c.enabled()) {
            if (c.chinaHat.get()) flags |= DevsPeerPayload.FLAG_CHINA_HAT;
            if (c.trail.get()) flags |= DevsPeerPayload.FLAG_TRAIL;
            if (c.halo.get()) flags |= DevsPeerPayload.FLAG_HALO;
            if (c.rings.get()) flags |= DevsPeerPayload.FLAG_RINGS;
            if (c.orbs.get()) flags |= DevsPeerPayload.FLAG_ORBS;
            if (c.mahoraga.get()) flags |= DevsPeerPayload.FLAG_MAHORAGA;
            color = c.color.get() | 0xFF000000;
        } else {
            flags = DevsPeerPayload.FLAG_ENABLED;
        }

        UUID self = mc.player.getUuid();
        ClientPlayNetworking.send(new DevsPeerPayload(self, flags, color));
    }
}
