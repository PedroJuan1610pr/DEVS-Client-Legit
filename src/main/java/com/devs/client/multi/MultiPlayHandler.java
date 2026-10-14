package com.devs.client.multi;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * ClientPlayPacketListener vía proxy: solo reacciona a chat/posición/disconnect.
 * Evita implementar a mano los 80+ métodos del interface en cada versión.
 */
public final class MultiPlayHandler implements InvocationHandler {
    private final MultiSession session;
    private final ClientConnection connection;

    private MultiPlayHandler(MultiSession session, ClientConnection connection) {
        this.session = session;
        this.connection = connection;
    }

    public static ClientPlayPacketListener create(MultiSession session, ClientConnection connection) {
        MultiPlayHandler h = new MultiPlayHandler(session, connection);
        return (ClientPlayPacketListener) Proxy.newProxyInstance(
            ClientPlayPacketListener.class.getClassLoader(),
            new Class<?>[]{ClientPlayPacketListener.class},
            h
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        try {
            if ("onDisconnected".equals(name) && args != null && args.length >= 1) {
                Text reason = args[0] instanceof Text t ? t : null;
                session.fail(reason == null ? "Play disconnected" : reason.getString());
                return null;
            }
            if ("isConnectionOpen".equals(name)) {
                return connection.isOpen();
            }
            if ("onGameMessage".equals(name) || "onChatMessage".equals(name) || "onProfilelessChatMessage".equals(name)) {
                if (args != null) {
                    for (Object a : args) {
                        if (a instanceof Text t) {
                            session.onInboundChat(t.getString());
                            break;
                        }
                    }
                }
                return null;
            }
            if ("onPlayerPositionLook".equals(name) || "onEntityPosition".equals(name)) {
                if (args != null && args.length > 0) {
                    Object pkt = args[0];
                    Double x = callD(pkt, "getX");
                    Double y = callD(pkt, "getY");
                    Double z = callD(pkt, "getZ");
                    if (x != null && y != null && z != null) {
                        session.onPlayPosition(x, y, z);
                    }
                    Integer tid = callI(pkt, "getTeleportId");
                    if (tid != null) {
                        try {
                            Class<?> conf = Class.forName("net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket");
                            Object packet = conf.getConstructor(int.class).newInstance(tid);
                            connection.send((Packet<?>) packet);
                        } catch (Throwable ignored) {}
                    }
                }
                return null;
            }
            // default no-op for other packets
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == long.class) return 0L;
            if (method.getReturnType() == float.class) return 0f;
            if (method.getReturnType() == double.class) return 0d;
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Double callD(Object o, String m) {
        try {
            Method method = o.getClass().getMethod(m);
            Object r = method.invoke(o);
            if (r instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {}
        return null;
    }

    private static Integer callI(Object o, String m) {
        try {
            Method method = o.getClass().getMethod(m);
            Object r = method.invoke(o);
            if (r instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {}
        return null;
    }
}
