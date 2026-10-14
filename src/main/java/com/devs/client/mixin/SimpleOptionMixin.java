package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin {
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void devs$fullbright(CallbackInfoReturnable<Object> cir) {
        if (!DevsClient.FULLBRIGHT.enabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        GameOptions o = mc.options;
        if (o == null || (Object) this != o.getGamma()) return;   // solo la opción de gamma
        cir.setReturnValue(DevsClient.FULLBRIGHT.gamma.get());
    }
}
