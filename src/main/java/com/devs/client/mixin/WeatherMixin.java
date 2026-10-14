package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WeatherMixin {
    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void devs$noRain(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientWorld)) return;
        if (DevsClient.WEATHER.enabled() && DevsClient.WEATHER.noRain.get()) cir.setReturnValue(0.0f);
        else if (DevsClient.NO_RENDER.hideWeather()) cir.setReturnValue(0.0f);
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void devs$noThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientWorld)) return;
        if (DevsClient.WEATHER.enabled() && DevsClient.WEATHER.noThunder.get()) cir.setReturnValue(0.0f);
        else if (DevsClient.NO_RENDER.hideWeather()) cir.setReturnValue(0.0f);
    }
}
