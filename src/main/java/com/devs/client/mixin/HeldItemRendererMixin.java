package com.devs.client.mixin;

import com.devs.client.DevsClient;
import com.devs.client.module.Viewmodel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Viewmodel: transforma el item de primera persona.
 * Firma 1.21.11: renderFirstPersonItem(..., MatrixStack, OrderedRenderCommandQueue, int)
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void devs$viewmodelHead(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                    float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                    OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        Viewmodel vm = DevsClient.VIEWMODEL;
        if (vm == null || !vm.active() || matrices == null) return;
        boolean main = hand == Hand.MAIN_HAND;
        if (main && !vm.mainHand.get()) return;
        if (!main && !vm.offHand.get()) return;

        float x = (main ? vm.mainX : vm.offX).get().floatValue();
        float y = (main ? vm.mainY : vm.offY).get().floatValue();
        float z = (main ? vm.mainZ : vm.offZ).get().floatValue();
        float scale = 1f + (main ? vm.mainScale : vm.offScale).get().floatValue() * 0.25f;
        float rx = (main ? vm.mainRotX : vm.offRotX).get().floatValue();
        float ry = (main ? vm.mainRotY : vm.offRotY).get().floatValue();
        float rz = (main ? vm.mainRotZ : vm.offRotZ).get().floatValue();

        matrices.translate(x, y, z);
        if (Math.abs(scale - 1f) > 0.001f) matrices.scale(scale, scale, scale);
        if (rx != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
        if (ry != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
        if (rz != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
    }
}
