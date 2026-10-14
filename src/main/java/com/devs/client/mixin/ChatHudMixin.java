package com.devs.client.mixin;

import com.devs.client.DevsClient;
import com.devs.client.module.ChatMentions;
import net.minecraft.client.chat.MessageSource;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/chat/MessageSource;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private void devs$antiSpam(Text message, MessageSignatureData sig, MessageSource source,
                               MessageIndicator indicator, CallbackInfo ci) {
        try {
            if (message != null && DevsClient.CHAT_UTILS != null && DevsClient.CHAT_UTILS.shouldBlock(message))
                ci.cancel();
        } catch (Throwable ignored) {}
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/chat/MessageSource;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0
    )
    private Text devs$process(Text message) {
        try {
            if (message == null || DevsClient.CHAT_UTILS == null) return message;
            Text t = DevsClient.CHAT_UTILS.process(message);
            if (ChatMentions.INSTANCE != null) t = ChatMentions.INSTANCE.process(t);
            return t;
        } catch (Throwable ignored) {
            return message;
        }
    }
}
