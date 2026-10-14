package com.devs.client.mixin;

import com.devs.client.DevsClient;
import com.devs.client.module.ComboCounter;
import com.devs.client.module.SessionStats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class AttackHookMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void devs$hitLog(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player != MinecraftClient.getInstance().player) return;
        DevsClient.HIT_LOG.onAttack(target);
        if (DevsClient.DAMAGE_NUMBERS != null) DevsClient.DAMAGE_NUMBERS.onAttack(target);
        for (var m : DevsClient.MODULES.all()) {
            if (m instanceof ComboCounter cc && m.enabled()) cc.onAttack(target);
            if (m instanceof SessionStats ss && m.enabled()) ss.onHitLanded();
        }
    }
}
