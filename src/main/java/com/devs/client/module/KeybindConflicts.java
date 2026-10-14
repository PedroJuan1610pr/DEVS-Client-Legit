package com.devs.client.module;

import com.devs.client.DevsClient;
import com.devs.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Detecta teclas de módulos en conflicto. */
public final class KeybindConflicts extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting scan = add(new BooleanSetting("Scan Now", false));
    public final BooleanSetting autoScan = add(new BooleanSetting("Scan On Enable", true));

    public KeybindConflicts() {
        super("Keybind Conflicts", "Detects modules sharing the same keybind.", "Misc");
    }

    @Override protected void onEnable() {
        if (autoScan.get()) doScan();
    }

    @Override public void tick() {
        if (scan.get()) {
            scan.set(false);
            doScan();
        }
    }

    private void doScan() {
        Map<Integer, List<String>> map = new HashMap<>();
        for (Module m : DevsClient.MODULES.all()) {
            int k = m.keybind();
            if (k < 0) continue;
            map.computeIfAbsent(k, x -> new ArrayList<>()).add(m.name());
        }
        int conflicts = 0;
        if (mc.player == null) return;
        for (var e : map.entrySet()) {
            if (e.getValue().size() < 2) continue;
            conflicts++;
            String keyName;
            try { keyName = GLFW.glfwGetKeyName(e.getKey(), 0); }
            catch (Throwable t) { keyName = null; }
            if (keyName == null) keyName = "key " + e.getKey();
            mc.player.sendMessage(Text.literal("[DEVS] Conflict " + keyName + ": " + String.join(", ", e.getValue()))
                .formatted(Formatting.RED));
        }
        if (conflicts == 0)
            mc.player.sendMessage(Text.literal("[DEVS] No keybind conflicts").formatted(Formatting.GREEN));
        else
            Notifications.push("Keybinds", conflicts + " conflicts", 0xFFFF4D6D);
    }
}
