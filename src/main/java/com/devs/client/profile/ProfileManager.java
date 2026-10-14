package com.devs.client.profile;

import com.devs.client.DevsClient;
import com.devs.client.module.Module;
import com.devs.client.module.Profiles;
import com.devs.client.setting.*;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Guarda y carga el estado de todos los módulos (activado + settings) en JSON. */
public final class ProfileManager {
    private static final Logger LOG = LoggerFactory.getLogger("devs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ProfileManager() {}

    /** .../config/devs/profiles */
    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("profiles");
    }

    private static String fileName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".json";
    }

    public static boolean exists(String name) {
        return Files.isRegularFile(dir().resolve(fileName(name)));
    }

    public static boolean save(String name) {
        try {
            JsonObject root = new JsonObject();
            for (Module m : DevsClient.MODULES.all()) {
                if (m instanceof Profiles) continue;
                JsonObject jm = new JsonObject();
                jm.addProperty("enabled", m.enabled());
                jm.addProperty("keybind", m.keybind());
                JsonObject js = new JsonObject();
                for (Setting<?> s : m.settings()) {
                    if (s instanceof BooleanSetting b) js.addProperty(s.name(), b.get());
                    else if (s instanceof NumberSetting n) js.addProperty(s.name(), n.get());
                    else if (s instanceof ModeSetting ms) js.addProperty(s.name(), ms.get());
                    else if (s instanceof ColorSetting c) js.addProperty(s.name(), c.get());
                    else if (s instanceof StringSetting ss) js.addProperty(s.name(), ss.get());
                }
                jm.add("settings", js);
                root.add(m.name(), jm);
            }
            Files.createDirectories(dir());
            Path target = dir().resolve(fileName(name));
            Path tmp = dir().resolve(fileName(name) + ".tmp");
            Files.writeString(tmp, GSON.toJson(root));
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            LOG.error("[DEVS] Could not save profile '{}'", name, e);
            return false;
        }
    }

    public static boolean load(String name) {
        Path file = dir().resolve(fileName(name));
        if (!Files.isRegularFile(file)) return false;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Module m : DevsClient.MODULES.all()) {
                if (m instanceof Profiles) continue;
                JsonElement el = root.get(m.name());
                if (el == null || !el.isJsonObject()) continue;
                JsonObject jm = el.getAsJsonObject();
                JsonObject js = jm.has("settings") && jm.get("settings").isJsonObject()
                    ? jm.getAsJsonObject("settings") : new JsonObject();
                for (Setting<?> s : m.settings()) apply(s, js.get(s.name()));
                if (jm.has("keybind") && jm.get("keybind").isJsonPrimitive()) {
                    try { m.setKeybind(jm.get("keybind").getAsInt()); } catch (RuntimeException ignored) {}
                }
                if (jm.has("enabled") && jm.get("enabled").isJsonPrimitive()) {
                    try {
                        boolean want = jm.get("enabled").getAsBoolean();
                        if (want != m.enabled()) m.toggle();
                    } catch (RuntimeException ignored) {}
                }
            }
            return true;
        } catch (Exception e) {
            LOG.error("[DEVS] Could not load profile '{}'", name, e);
            return false;
        }
    }

    /** Aplica un valor del JSON validándolo; un valor malo se ignora sin romper el resto. */
    private static void apply(Setting<?> s, JsonElement el) {
        if (el == null || !el.isJsonPrimitive()) return;
        JsonPrimitive p = el.getAsJsonPrimitive();
        try {
            if (s instanceof BooleanSetting b && p.isBoolean()) b.set(p.getAsBoolean());
            else if (s instanceof NumberSetting n && p.isNumber())
                n.set(Math.max(n.min(), Math.min(n.max(), p.getAsDouble())));
            else if (s instanceof ModeSetting m && p.isString() && m.modes().contains(p.getAsString()))
                m.set(p.getAsString());
            else if (s instanceof ColorSetting c && p.isNumber()) c.set(p.getAsInt());
            else if (s instanceof StringSetting ss && p.isString()) ss.set(p.getAsString());
        } catch (RuntimeException ignored) {}
    }
}
