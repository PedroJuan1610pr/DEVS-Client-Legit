package com.devs.client.friends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Lista de friends persistente (nombre en minúsculas + UUID si se conoce). */
public final class FriendsManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> NAMES = new LinkedHashSet<>();
    private static final Set<UUID> UUIDS = new LinkedHashSet<>();
    private static boolean loaded;

    private FriendsManager() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("friends.json");
    }

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            Path f = file();
            if (!Files.isRegularFile(f)) return;
            JsonArray arr = JsonParser.parseString(Files.readString(f)).getAsJsonArray();
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) addName(el.getAsString());
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            ensureLoaded();
            JsonArray arr = new JsonArray();
            for (String n : NAMES) arr.add(n);
            Path dir = file().getParent();
            Files.createDirectories(dir);
            Files.writeString(file(), GSON.toJson(arr));
        } catch (Exception ignored) {}
    }

    public static Set<String> names() {
        ensureLoaded();
        return Collections.unmodifiableSet(NAMES);
    }

    public static void addName(String name) {
        if (name == null || name.isBlank()) return;
        ensureLoaded();
        NAMES.add(name.trim().toLowerCase(Locale.ROOT));
        save();
    }

    public static void removeName(String name) {
        if (name == null) return;
        ensureLoaded();
        NAMES.remove(name.trim().toLowerCase(Locale.ROOT));
        save();
    }

    /** @return true si tras el toggle es friend */
    public static boolean toggle(PlayerEntity p) {
        if (p == null) return false;
        ensureLoaded();
        String n = p.getName().getString().toLowerCase(Locale.ROOT);
        if (NAMES.contains(n) || UUIDS.contains(p.getUuid())) {
            NAMES.remove(n);
            UUIDS.remove(p.getUuid());
            save();
            return false;
        }
        NAMES.add(n);
        UUIDS.add(p.getUuid());
        save();
        return true;
    }

    public static boolean isFriend(PlayerEntity p) {
        if (p == null) return false;
        ensureLoaded();
        if (UUIDS.contains(p.getUuid())) return true;
        return NAMES.contains(p.getName().getString().toLowerCase(Locale.ROOT));
    }

    public static boolean isFriendName(String name) {
        if (name == null) return false;
        ensureLoaded();
        return NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    public static void clear() {
        ensureLoaded();
        NAMES.clear();
        UUIDS.clear();
        save();
    }
}
