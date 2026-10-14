package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.GizmoDrawing.CollectorScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method="collectGizmos", at=@At("HEAD"))
    private void devs$renderFinderGizmos(CallbackInfo ci){
        try(CollectorScope ignored=((WorldRenderer)(Object)this).startDrawingGizmos()) { DevsClient.renderFinders(); }
    }
}
