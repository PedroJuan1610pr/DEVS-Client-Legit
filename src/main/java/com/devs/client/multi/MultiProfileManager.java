package com.devs.client.multi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MultiProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MultiProfileManager INSTANCE;
    private final List<MultiProfile> profiles = new CopyOnWriteArrayList<>();

    public static MultiProfileManager get() {
        if (INSTANCE == null) INSTANCE = new MultiProfileManager();
        return INSTANCE;
    }

    private MultiProfileManager() { load(); }

    public List<MultiProfile> all() { return Collections.unmodifiableList(profiles); }

    public MultiProfile findByName(String name) {
        if (name == null) return null;
        for (MultiProfile p : profiles) {
            if (name.equalsIgnoreCase(p.name)) return p;
        }
        return null;
    }

    public MultiProfile findById(String id) {
        if (id == null) return null;
        for (MultiProfile p : profiles) {
            if (id.equals(p.id)) return p;
        }
        return null;
    }

    public synchronized MultiProfile create(String name) {
        if (name == null || name.isBlank()) return null;
        if (findByName(name) != null) return null;
        MultiProfile p = new MultiProfile();
        p.name = name.trim();
        profiles.add(p);
        save();
        return p;
    }

    public synchronized boolean delete(String id) {
        boolean ok = profiles.removeIf(p -> p.id.equals(id));
        if (ok) save();
        return ok;
    }

    public synchronized void saveProfile(MultiProfile p) {
        if (p == null) return;
        p.normalize();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id.equals(p.id)) {
                profiles.set(i, p);
                save();
                return;
            }
        }
        profiles.add(p);
        save();
    }

    private Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("multi-profiles.json");
    }

    public void load() {
        profiles.clear();
        try {
            Path f = file();
            if (!Files.isRegularFile(f)) return;
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("profiles");
            if (arr == null) return;
            for (JsonElement el : arr) {
                if (el.isJsonObject()) profiles.add(MultiProfile.fromJson(el.getAsJsonObject()));
            }
        } catch (Throwable ignored) {}
    }

    public void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (MultiProfile p : profiles) arr.add(p.toJson());
            root.add("profiles", arr);
            Files.writeString(f, GSON.toJson(root));
        } catch (Throwable ignored) {}
    }
}
