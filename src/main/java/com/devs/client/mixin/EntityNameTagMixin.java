package com.devs.client.mixin;

import com.devs.client.module.Nametags;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Oculta el nametag vanilla cuando Nametags del cliente lo reemplaza. */
@Mixin(EntityRenderer.class)
public class EntityNameTagMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void devs$suppressVanillaNameTag(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (state == null || entity == null) return;
        if (Nametags.shouldSuppressVanilla(entity)) {
            state.displayName = null;
            state.nameLabelPos = null;
        }
    }
}
