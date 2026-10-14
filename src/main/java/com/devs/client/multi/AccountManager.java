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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AccountManager INSTANCE;
    private final List<DevsAccount> accounts = new CopyOnWriteArrayList<>();

    public static AccountManager get() {
        if (INSTANCE == null) INSTANCE = new AccountManager();
        return INSTANCE;
    }

    private AccountManager() {
        load();
    }

    public List<DevsAccount> all() {
        return Collections.unmodifiableList(accounts);
    }

    public DevsAccount findById(String id) {
        if (id == null) return null;
        for (DevsAccount a : accounts) {
            if (id.equals(a.id)) return a;
        }
        return null;
    }

    public DevsAccount findByName(String name) {
        if (name == null) return null;
        for (DevsAccount a : accounts) {
            if (name.equalsIgnoreCase(a.displayName())) return a;
        }
        return null;
    }

    public synchronized boolean add(String label) {
        if (label == null || label.isBlank()) return false;
        String t = label.trim();
        for (DevsAccount a : accounts) {
            if (t.equalsIgnoreCase(a.label) || t.equalsIgnoreCase(a.username)) return false;
        }
        accounts.add(new DevsAccount(t));
        save();
        return true;
    }

    public synchronized boolean remove(String id) {
        boolean ok = accounts.removeIf(a -> a.id.equals(id));
        if (ok) save();
        return ok;
    }

    public synchronized boolean rename(String id, String newLabel) {
        DevsAccount a = findById(id);
        if (a == null || newLabel == null || newLabel.isBlank()) return false;
        String t = newLabel.trim();
        for (DevsAccount o : accounts) {
            if (o != a && t.equalsIgnoreCase(o.label)) return false;
        }
        a.label = t;
        a.username = t;
        a.uuid = net.minecraft.util.Uuids.getOfflinePlayerUuid(t).toString();
        save();
        return true;
    }

    private Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("multi-accounts.json");
    }

    public void load() {
        accounts.clear();
        try {
            Path f = file();
            if (!Files.isRegularFile(f)) return;
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("accounts");
            if (arr == null) return;
            for (JsonElement el : arr) {
                if (el.isJsonObject()) accounts.add(DevsAccount.fromJson(el.getAsJsonObject()));
            }
        } catch (Throwable ignored) {}
    }

    public void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (DevsAccount a : accounts) arr.add(a.toJson());
            root.add("accounts", arr);
            Files.writeString(f, GSON.toJson(root));
        } catch (Throwable ignored) {}
    }
}
