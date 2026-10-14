package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin {
    // Constructor firma puede variar; intentamos HEAD del init
    @Inject(method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/text/Text;Lnet/minecraft/network/DisconnectionInfo;)V",
            at = @At("RETURN"), require = 0)
    private void devs$onDisconnectInfo(Screen parent, Text title, DisconnectionInfo info, CallbackInfo ci) {
        DevsClient.AUTO_RECONNECT.onDisconnect();
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/text/Text;Lnet/minecraft/text/Text;)V",
            at = @At("RETURN"), require = 0)
    private void devs$onDisconnectText(Screen parent, Text title, Text reason, CallbackInfo ci) {
        DevsClient.AUTO_RECONNECT.onDisconnect();
    }
}
