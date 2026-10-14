package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class NoRenderTotemMixin {
    @Inject(method = "setFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void devs$noTotem(ItemStack stack, Random random, CallbackInfo ci) {
        if (DevsClient.NO_RENDER.hideTotem() && stack.is(Items.TOTEM_OF_UNDYING)) ci.cancel();
    }
}
