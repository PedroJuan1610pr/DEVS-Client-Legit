package com.devs.client.multi;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientLoginPacketListener;
import net.minecraft.text.Text;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Listener de login (proxy), mismo enfoque que Autism MultiSession.listener(). */
public final class MultiLoginHandler implements InvocationHandler {
    private static final Map<Object, MultiLoginHandler> BY_PROXY = new ConcurrentHashMap<>();

    private final MultiSession session;
    private final String host;
    private final int port;
    private volatile ClientConnection connection;

    private MultiLoginHandler(MultiSession session, String host, int port) {
        this.session = session;
        this.host = host;
        this.port = port;
    }

    public static ClientLoginPacketListener create(MultiSession session, String host, int port) {
        MultiLoginHandler h = new MultiLoginHandler(session, host, port);
        ClientLoginPacketListener proxy = (ClientLoginPacketListener) Proxy.newProxyInstance(
            ClientLoginPacketListener.class.getClassLoader(),
            new Class<?>[]{ClientLoginPacketListener.class},
            h
        );
        BY_PROXY.put(proxy, h);
        return proxy;
    }

    public static void bindConnection(Object proxy, ClientConnection conn) {
        MultiLoginHandler h = BY_PROXY.get(proxy);
        if (h != null) {
            h.connection = conn;
            h.session.bindConnection(conn);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        try {
            if ("onHello".equals(name)) {
                session.fail("Server is online-mode (cracked bots need offline-mode)");
                return null;
            }
            if ("onSuccess".equals(name) || "onLoginSuccess".equals(name)) {
                session.onLoginSuccess();
                // play listener
                ClientConnection conn = connection;
                if (conn != null) {
                    Object play = MultiPlayHandler.create(session, conn);
                    try {
                        Method m = ClientConnection.class.getMethod("setInitialPacketListener",
                            Class.forName("net.minecraft.network.listener.PacketListener"));
                        m.invoke(conn, play);
                    } catch (Throwable ignored) {}
                }
                return null;
            }
            if ("onDisconnect".equals(name) || "onDisconnected".equals(name)) {
                session.fail(extractReason(args));
                return null;
            }
            if ("onCompression".equals(name) && args != null && args.length > 0) {
                try {
                    Method gt = args[0].getClass().getMethod("getCompressionThreshold");
                    Object thr = gt.invoke(args[0]);
                    if (thr instanceof Number n && connection != null && n.intValue() >= 0) {
                        connection.setCompressionThreshold(n.intValue(), false);
                    }
                } catch (Throwable ignored) {}
                return null;
            }
            if ("isConnectionOpen".equals(name) || "isAcceptingMessages".equals(name) || "shouldHandleMessage".equals(name)) {
                return connection != null && connection.isOpen();
            }
            if ("tickLogin".equals(name) || "tick".equals(name)
                || "onCookieRequest".equals(name) || "onQueryRequest".equals(name)) {
                return null;
            }
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) return false;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == float.class) return 0f;
            if (rt == double.class) return 0d;
            return null;
        } catch (Throwable t) {
            return null;
        }
    }


    /** Usado por MultiConfigHandler (si existe) para cambiar el packet listener. */
    public static boolean setListener(ClientConnection conn, Object state, Object listener) {
        if (conn == null || listener == null) return false;
        for (Method m : ClientConnection.class.getDeclaredMethods()) {
            if (!"setPacketListener".equals(m.getName())) continue;
            try {
                m.setAccessible(true);
                if (m.getParameterCount() == 2 && state != null) {
                    m.invoke(conn, state, listener);
                    return true;
                }
                if (m.getParameterCount() == 1) {
                    m.invoke(conn, listener);
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        try {
            Method m = ClientConnection.class.getMethod(
                "setInitialPacketListener",
                Class.forName("net.minecraft.network.listener.PacketListener")
            );
            m.invoke(conn, listener);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static String extractReason(Object[] args) {
        if (args == null) return "Disconnected";
        for (Object a : args) {
            if (a instanceof Text t) return t.getString();
            if (a == null) continue;
            for (String m : new String[]{"reason", "getReason", "message"}) {
                try {
                    Object v = a.getClass().getMethod(m).invoke(a);
                    if (v instanceof Text t) return t.getString();
                    if (v != null) return v.toString();
                } catch (Throwable ignored) {}
            }
        }
        return "Disconnected";
    }
}
