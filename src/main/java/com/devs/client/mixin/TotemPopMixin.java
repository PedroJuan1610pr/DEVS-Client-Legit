package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Estado de entidad 35 = totem de la inmortalidad reventado. El cliente vanilla lo gestiona directamente
 * en ClientPlayNetworkHandler.onEntityStatus y NO llega a LivingEntity.handleStatus,
 * por eso hay que engancharse aqui.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class TotemPopMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void devs$totemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 35) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread() || mc.world == null) return;
        Entity e = packet.getEntity(mc.world);
        if (e instanceof PlayerEntity p) DevsClient.TOTEM_POPS.onPop(p);
    }
}
