package com.devs.client.mixin;

import com.devs.client.DevsClient;
import com.devs.client.module.FreeLook;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.1: Camera.update() fue reemplazado por updateRenderState. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "updateRenderState", at = @At("RETURN"), require = 0)
    private void devs$cameraOverride(CameraRenderState state, float tickProgress, CallbackInfo ci) {
        FreeLook fl = DevsClient.FREE_LOOK;
        if (fl != null && fl.enabled()) {
            setRotation(fl.cameraYaw, fl.cameraPitch);
        }
    }
}
