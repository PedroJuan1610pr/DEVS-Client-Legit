package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FreecamLookMixin {
    /** FreeLook: el ratón no gira al personaje. */
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void devs$lookOverride(double dx, double dy, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        if (DevsClient.FREE_LOOK.enabled() && DevsClient.FREE_LOOK.onMouse(dx, dy)) {
            ci.cancel();
        }
    }
}
