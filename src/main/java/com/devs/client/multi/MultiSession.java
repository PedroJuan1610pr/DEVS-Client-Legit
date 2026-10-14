package com.devs.client.multi;

import io.netty.channel.ChannelFuture;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.network.listener.ClientLoginPacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Flujo de conexión copiado de Autism MultiSession:
 * new ClientConnection(CLIENTBOUND) → connect(addr, NetworkingBackend.remote(...), conn)
 * → setInitialPacketListener → Handshake + LoginHello
 */
public final class MultiSession {
    public enum Status {
        IDLE, RESOLVING, CONNECTING, LOGIN, PLAY, READY, FAILED, DISCONNECTED
    }

    public interface Sink {
        void onStatus(MultiSession session);
        void onChat(MultiSession session, String message);
        void onSystem(String message);
    }

    private final long generation;
    private final DevsAccount account;
    private final String serverAddress;
    private final Sink sink;
    private final AtomicReference<Status> status = new AtomicReference<>(Status.IDLE);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile String detail = "";
    private volatile ClientConnection connection;
    private volatile double x, y, z;
    private volatile boolean hasPosition;

    public MultiSession(long generation, DevsAccount account, String serverAddress, Sink sink) {
        this.generation = generation;
        this.account = account;
        this.serverAddress = serverAddress == null ? "localhost" : serverAddress.trim();
        this.sink = sink;
    }

    public long generation() { return generation; }
    public DevsAccount account() { return account; }
    public String accountId() { return account.id; }
    public String name() { return account.displayName(); }
    public Status status() { return status.get(); }
    public String detail() { return detail; }
    public boolean connected() {
        ClientConnection c = connection;
        return c != null && c.isOpen() && !closed.get();
    }
    public boolean ready() { return status.get() == Status.READY && connected(); }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public boolean hasPosition() { return hasPosition; }

    public void bindConnection(ClientConnection conn) {
        this.connection = conn;
    }

    public void start(java.util.concurrent.Executor worker) {
        if (closed.get()) return;
        setStatus(Status.RESOLVING, "Resolving " + serverAddress);
        worker.execute(() -> {
            try {
                ServerAddress addr = ServerAddress.parse(serverAddress);
                String host = addr.getAddress();
                int port = addr.getPort();
                setStatus(Status.CONNECTING, host + ":" + port);

                InetSocketAddress socket = new InetSocketAddress(java.net.InetAddress.getByName(host), port);

                // Autism: new Connection(PacketFlow.CLIENTBOUND)
                ClientConnection created = new ClientConnection(NetworkSide.CLIENTBOUND);
                connection = created;

                ClientLoginPacketListener loginListener = MultiLoginHandler.create(this, host, port);
                MultiLoginHandler.bindConnection(loginListener, created);

                // Prefer setInitialPacketListener (público en 26.x) antes del connect
                setInitialListener(created, loginListener);

                boolean useNative = useNativeTransport();
                // Autism: EventLoopGroupHolder.remote(...) → Yarn: NetworkingBackend.remote(useEpoll)
                NetworkingBackend backend = NetworkingBackend.remote(useNative);

                ChannelFuture future = ClientConnection.connect(socket, backend, created);
                future.addListener(done -> {
                    if (closed.get()) {
                        try {
                            created.disconnect(Text.literal("Cancelled"));
                            created.handleDisconnection();
                        } catch (Throwable ignored) {}
                        return;
                    }
                    if (!done.isSuccess()) {
                        fail(shortErr(done.cause()));
                        return;
                    }
                    try {
                        setStatus(Status.LOGIN, "Logging in as " + account.displayName());
                        // Autism: initiateServerboundPlayConnection + ServerboundHelloPacket
                        // Yarn 26.1.2: Handshake + LoginHello tras canal listo
                        int protocol = SharedConstants.getProtocolVersion();
                        created.send(new HandshakeC2SPacket(protocol, host, port, ConnectionIntent.LOGIN));
                        created.send(new LoginHelloC2SPacket(account.displayName(), account.profileUuid()));
                    } catch (Throwable t) {
                        fail(shortErr(t));
                    }
                });
            } catch (Throwable t) {
                fail(shortErr(t));
            }
        });
    }

    private static boolean useNativeTransport() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            Object options = mc.options;
            // shouldUseNativeTransport() / getUseNativeTransport()
            for (String name : new String[]{"shouldUseNativeTransport", "getUseNativeTransport", "useNativeTransport"}) {
                try {
                    Method m = options.getClass().getMethod(name);
                    Object v = m.invoke(options);
                    if (v instanceof Boolean b) return b;
                    // SimpleOption
                    try {
                        Method get = v.getClass().getMethod("getValue");
                        Object g = get.invoke(v);
                        if (g instanceof Boolean b) return b;
                    } catch (Throwable ignored) {}
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return true;
    }

    private static void setInitialListener(ClientConnection conn, Object listener) {
        try {
            Method m = ClientConnection.class.getMethod("setInitialPacketListener",
                Class.forName("net.minecraft.network.listener.PacketListener"));
            m.invoke(conn, listener);
            return;
        } catch (Throwable ignored) {}
        // fallback private setPacketListener
        try {
            Object s2c = Class.forName("net.minecraft.network.state.LoginStates")
                .getField("S2C").get(null);
            Method m = ClientConnection.class.getDeclaredMethod(
                "setPacketListener",
                Class.forName("net.minecraft.network.state.NetworkState"),
                Class.forName("net.minecraft.network.listener.PacketListener")
            );
            m.setAccessible(true);
            m.invoke(conn, s2c, listener);
        } catch (Throwable ignored) {}
    }

    void onLoginSuccess() {
        if (closed.get()) return;
        setStatus(Status.PLAY, "Joined");
        setStatus(Status.READY, "Ready");
    }

    void onPlayPosition(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
        this.hasPosition = true;
        if (status.get() == Status.PLAY) setStatus(Status.READY, "Ready");
    }

    void onInboundChat(String msg) {
        if (sink != null) sink.onChat(this, msg);
    }

    public void sendChat(String message) {
        if (!ready() || message == null || message.isBlank()) return;
        try {
            ClientConnection c = connection;
            if (c == null) return;
            String m = message.trim();
            c.send(new CommandExecutionC2SPacket(m.startsWith("/") ? m.substring(1) : "say " + m));
        } catch (Throwable ignored) {}
    }

    public void sendCommand(String command) {
        if (!ready() || command == null || command.isBlank()) return;
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        try {
            ClientConnection conn = connection;
            if (conn != null) conn.send(new CommandExecutionC2SPacket(cmd));
        } catch (Throwable ignored) {}
    }

    public void disconnect(String reason) {
        if (!closed.compareAndSet(false, true)) return;
        try {
            ClientConnection c = connection;
            if (c != null) {
                c.disconnect(Text.literal(reason == null ? "Disconnected" : reason));
                try { c.handleDisconnection(); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        connection = null;
        setStatus(Status.DISCONNECTED, reason == null ? "Disconnected" : reason);
    }

    void fail(String reason) {
        closed.set(true);
        try {
            ClientConnection c = connection;
            if (c != null) {
                c.disconnect(Text.literal(reason));
                try { c.handleDisconnection(); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        connection = null;
        setStatus(Status.FAILED, reason);
    }

    private void setStatus(Status s, String d) {
        status.set(s);
        detail = d == null ? "" : d;
        if (sink != null) {
            try { sink.onStatus(this); } catch (Throwable ignored) {}
        }
    }

    static String shortErr(Throwable t) {
        if (t == null) return "Unknown error";
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        String m = c.getMessage();
        if (m == null || m.isBlank()) m = c.getClass().getSimpleName();
        if (m.length() > 140) m = m.substring(0, 140) + "…";
        return c.getClass().getSimpleName() + ": " + m;
    }
}
