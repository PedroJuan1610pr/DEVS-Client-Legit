package com.devs.client.mixin;

import com.devs.client.net.PeerTracker;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void devs$tabIcon(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        try {
            if (entry == null || entry.getProfile() == null) return;
            if (!PeerTracker.has(entry.getProfile().id())) return;
            Text base = cir.getReturnValue();
            if (base == null) return;
            cir.setReturnValue(
                Text.literal("✦ ").formatted(Formatting.AQUA).append(base)
            );
        } catch (Throwable ignored) {}
    }
}
