package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.1: tiltViewWhenHurt(CameraRenderState, MatrixStack) */
@Mixin(GameRenderer.class)
public abstract class NoRenderHurtCamMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void devs$noHurt(CameraRenderState camera, MatrixStack matrices, CallbackInfo ci) {
        if (DevsClient.NO_RENDER != null && DevsClient.NO_RENDER.hideHurtCam()) ci.cancel();
    }
}
