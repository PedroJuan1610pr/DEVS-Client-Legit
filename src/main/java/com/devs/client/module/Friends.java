package com.devs.client.module;

import com.devs.client.friends.FriendsManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Friends: middle-click en un jugador, o comandos:
 *   .friend add/remove/list/toggle [nick]
 */
public final class Friends extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final ColorSetting color = add(new ColorSetting("Friend Color", 0xFF55FF55));
    public final BooleanSetting clear = add(new BooleanSetting("Clear Friends", false));
    public final BooleanSetting listChat = add(new BooleanSetting("List In Chat", false));

    public Friends() {
        super("Friends", "Middle-click player or .friend add/remove/list [nick]", "Misc");
    }

    @Override public void tick() {
        FriendsManager.ensureLoaded();
        if (clear.get()) {
            clear.set(false);
            FriendsManager.clear();
            if (mc.player != null)
                mc.player.sendMessage(Text.literal("[DEVS] Friends cleared").formatted(Formatting.GRAY));
            Notifications.push("Friends", "Cleared");
        }
        if (listChat.get()) {
            listChat.set(false);
            if (mc.player != null) {
                var names = FriendsManager.names();
                mc.player.sendMessage(Text.literal("[DEVS] Friends (" + names.size() + "): " + String.join(", ", names))
                    .formatted(Formatting.GREEN));
            }
        }
    }
}
