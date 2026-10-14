package com.devs.client.module;

import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Resalta menciones de tu nombre en chat y opcionalmente avisa. */
public final class ChatMentions extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting notify = add(new BooleanSetting("Toast Notify", true));
    public final BooleanSetting sound = add(new BooleanSetting("Play Sound", false));
    public final BooleanSetting highlight = add(new BooleanSetting("Prefix Highlight", true));

    public static ChatMentions INSTANCE;

    public ChatMentions() {
        super("Chat Mentions", "Highlights and notifies when your name appears in chat.", "Misc");
        INSTANCE = this;
    }

    /** Procesa mensaje entrante; devuelve el texto posiblemente modificado. */
    public Text process(Text original) {
        if (!enabled() || mc.player == null) return original;
        String raw = original.getString();
        String name = mc.player.getName().getString();
        if (name.isEmpty() || raw.toLowerCase().indexOf(name.toLowerCase()) < 0) return original;
        if (notify.get()) Notifications.push("Mention", raw.length() > 48 ? raw.substring(0, 48) + "…" : raw, 0xFFFFD94D);
        if (sound.get() && mc.player != null)
            mc.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.2f);
        if (highlight.get())
            return Text.literal("⚡ ").formatted(Formatting.GOLD).append(original);
        return original;
    }
}
