package com.devs.client;

import com.devs.client.net.DevsNetworking;
import net.fabricmc.api.ModInitializer;

/** Entrypoint común: registra el canal de red (relay en servidor Fabric). */
public final class DevsMain implements ModInitializer {
    @Override
    public void onInitialize() {
        DevsNetworking.registerCommon();
    }
}
