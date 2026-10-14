package com.devs.client.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Sincroniza presencia Devs + cosméticos entre clientes (vía servidor Fabric). */
public record DevsPeerPayload(UUID player, int flags, int color) implements CustomPayload {
    public static final Id<DevsPeerPayload> ID = new Id<>(Identifier.of("devs", "peer"));

    public static final int FLAG_ENABLED   = 1;
    public static final int FLAG_CHINA_HAT = 1 << 1;
    public static final int FLAG_TRAIL     = 1 << 2;
    public static final int FLAG_HALO      = 1 << 3;
    public static final int FLAG_RINGS     = 1 << 4;
    public static final int FLAG_ORBS      = 1 << 5;
    public static final int FLAG_MAHORAGA  = 1 << 6;

    public static final PacketCodec<RegistryByteBuf, DevsPeerPayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeUuid(value.player);
            buf.writeInt(value.flags);
            buf.writeInt(value.color);
        },
        buf -> new DevsPeerPayload(buf.readUuid(), buf.readInt(), buf.readInt())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public boolean has(int bit) {
        return (flags & bit) != 0;
    }
}
