package com.devs.client.module;

import com.devs.client.DevsClient;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Tecla de pánico: desactiva todos los módulos al instante. */
public final class Panic extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting hideGui = add(new BooleanSetting("Close GUI", true));
    public final BooleanSetting chatMsg = add(new BooleanSetting("Chat Message", true));

    private final List<String> lastEnabled = new ArrayList<>();

    public Panic() { super("Panic", "Disable all modules instantly (bind a key).", "Misc"); }

    @Override protected void onEnable() {
        lastEnabled.clear();
        for (Module m : DevsClient.MODULES.all()) {
            if (m == this) continue;
            if (m.enabled()) {
                lastEnabled.add(m.name());
                m.setEnabled(false);
            }
        }
        if (hideGui.get() && mc.currentScreen != null) mc.setScreen(null);
        if (chatMsg.get() && mc.player != null)
            mc.player.sendMessage(Text.literal("[DEVS] Panic — all modules off").formatted(Formatting.RED));
        Notifications.push("Panic", "All modules disabled", 0xFFFF4D6D);
        // turn self off again so it acts as a one-shot button
        setEnabled(false);
    }

    /** Restaura los que estaban activos antes del panic (opcional vía setting externo). */
    public void restore() {
        for (Module m : DevsClient.MODULES.all()) {
            if (lastEnabled.contains(m.name()) && !m.enabled()) m.setEnabled(true);
        }
        lastEnabled.clear();
    }
}
