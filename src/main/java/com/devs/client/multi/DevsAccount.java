package com.devs.client.multi;

import com.google.gson.JsonObject;
import net.minecraft.util.Uuids;

import java.util.UUID;

/** Cuenta cracked/offline (sin Microsoft). */
public final class DevsAccount {
    public String id = "";
    public String label = "";
    public String username = "";
    public String uuid = "";

    public DevsAccount() {}

    public DevsAccount(String label) {
        this.label = label == null ? "" : label.trim();
        this.username = this.label;
        this.uuid = Uuids.getOfflinePlayerUuid(this.username).toString();
        this.id = UUID.randomUUID().toString();
    }

    public String displayName() {
        return username != null && !username.isBlank() ? username : label;
    }

    public UUID profileUuid() {
        try {
            return UUID.fromString(uuid);
        } catch (Exception e) {
            return Uuids.getOfflinePlayerUuid(displayName());
        }
    }

    public void normalize() {
        if (label == null) label = "";
        label = label.trim();
        if (username == null || username.isBlank()) username = label;
        username = username.trim();
        if (uuid == null || uuid.isBlank()) {
            uuid = Uuids.getOfflinePlayerUuid(username).toString();
        }
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("label", label);
        o.addProperty("username", username);
        o.addProperty("uuid", uuid);
        return o;
    }

    public static DevsAccount fromJson(JsonObject o) {
        DevsAccount a = new DevsAccount();
        if (o.has("id")) a.id = o.get("id").getAsString();
        if (o.has("label")) a.label = o.get("label").getAsString();
        if (o.has("username")) a.username = o.get("username").getAsString();
        if (o.has("uuid")) a.uuid = o.get("uuid").getAsString();
        a.normalize();
        return a;
    }
}
