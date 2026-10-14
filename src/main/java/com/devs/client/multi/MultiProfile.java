package com.devs.client.multi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MultiProfile {
    public static final int MAX_SESSIONS = 64;

    public enum Pacing {
        Gentle(1, 1000),
        Balanced(4, 250),
        Fast(8, 100),
        Immediate(MAX_SESSIONS, 0);

        public final int concurrency;
        public final int delayMs;

        Pacing(int c, int d) { concurrency = c; delayMs = d; }
    }

    public String id = UUID.randomUUID().toString();
    public String name = "default";
    public String serverAddress = "localhost";
    public Pacing pacing = Pacing.Balanced;
    public final List<String> accountIds = new ArrayList<>();

    public MultiProfile() {}

    public MultiProfile(MultiProfile o) {
        this.id = o.id;
        this.name = o.name;
        this.serverAddress = o.serverAddress;
        this.pacing = o.pacing;
        this.accountIds.addAll(o.accountIds);
    }

    public void normalize() {
        if (name == null || name.isBlank()) name = "default";
        if (serverAddress == null) serverAddress = "localhost";
        serverAddress = serverAddress.trim();
        if (pacing == null) pacing = Pacing.Balanced;
        accountIds.removeIf(s -> s == null || s.isBlank());
        while (accountIds.size() > MAX_SESSIONS) accountIds.remove(accountIds.size() - 1);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("name", name);
        o.addProperty("server", serverAddress);
        o.addProperty("pacing", pacing.name());
        JsonArray arr = new JsonArray();
        for (String a : accountIds) arr.add(a);
        o.add("accounts", arr);
        return o;
    }

    public static MultiProfile fromJson(JsonObject o) {
        MultiProfile p = new MultiProfile();
        if (o.has("id")) p.id = o.get("id").getAsString();
        if (o.has("name")) p.name = o.get("name").getAsString();
        if (o.has("server")) p.serverAddress = o.get("server").getAsString();
        if (o.has("pacing")) {
            try { p.pacing = Pacing.valueOf(o.get("pacing").getAsString()); } catch (Exception ignored) {}
        }
        if (o.has("accounts") && o.get("accounts").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("accounts")) {
                p.accountIds.add(el.getAsString());
            }
        }
        p.normalize();
        return p;
    }
}
