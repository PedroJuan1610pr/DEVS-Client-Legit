package com.devs.client.setting;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lista de item ids CSV, editable con el picker de la GUI. */
public final class ItemListSetting extends StringSetting {

    public ItemListSetting(String name, String defaultCsv) {
        super(name, defaultCsv == null ? "" : defaultCsv, 1024);
    }

    public List<String> list() {
        return parse(get());
    }

    public int count() {
        return list().size();
    }

    public String summary() {
        int n = count();
        if (n == 0) return "None";
        if (n == 1) return list().get(0);
        return n + " items";
    }

    public void clear() {
        set("");
    }

    public void setList(List<String> ids) {
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String raw : ids) {
            String n = normalize(raw);
            if (n != null) uniq.add(n);
        }
        set(String.join(",", uniq));
    }

    public void addItem(String id) {
        String n = normalize(id);
        if (n == null) return;
        List<String> cur = list();
        if (!cur.contains(n)) {
            cur.add(n);
            setList(cur);
        }
    }

    public void removeItem(String id) {
        String n = normalize(id);
        if (n == null) return;
        List<String> cur = list();
        cur.removeIf(s -> {
            String ns = normalize(s);
            return ns != null && (ns.equals(n) || ns.equals("minecraft:" + n));
        });
        setList(cur);
    }

    public void toggle(String id) {
        String n = normalize(id);
        if (n == null) return;
        if (has(n)) removeItem(n);
        else addItem(n);
    }

    public boolean has(String id) {
        String n = normalize(id);
        if (n == null) return false;
        for (String s : list()) {
            String ns = normalize(s);
            if (ns == null) continue;
            if (ns.equals(n)) return true;
        }
        return false;
    }

    public boolean has(Item item) {
        return containsItem(item);
    }

    public boolean containsItem(Item item) {
        if (item == null || item == Items.AIR) return false;
        Identifier rid = Registries.ITEM.getId(item);
        if (rid == null) return false;
        return has(rid.getPath()) || has(rid.toString());
    }

    public Set<Item> toItemSet() {
        Set<Item> set = new LinkedHashSet<>();
        for (String s : list()) {
            Item it = resolve(s);
            if (it != null && it != Items.AIR) set.add(it);
        }
        return set;
    }

    /** Atajos comunes: xp, gap, pearl… */
    private static final java.util.Map<String, String> ALIASES = java.util.Map.ofEntries(
        java.util.Map.entry("xp", "experience_bottle"),
        java.util.Map.entry("exp", "experience_bottle"),
        java.util.Map.entry("xpbottle", "experience_bottle"),
        java.util.Map.entry("exp_bottle", "experience_bottle"),
        java.util.Map.entry("experience", "experience_bottle"),
        java.util.Map.entry("gap", "golden_apple"),
        java.util.Map.entry("gaps", "golden_apple"),
        java.util.Map.entry("gapple", "golden_apple"),
        java.util.Map.entry("egap", "enchanted_golden_apple"),
        java.util.Map.entry("godapple", "enchanted_golden_apple"),
        java.util.Map.entry("pearl", "ender_pearl"),
        java.util.Map.entry("pearls", "ender_pearl"),
        java.util.Map.entry("crystal", "end_crystal"),
        java.util.Map.entry("totem", "totem_of_undying"),
        java.util.Map.entry("obs", "obsidian"),
        java.util.Map.entry("obby", "obsidian"),
        java.util.Map.entry("splash", "splash_potion"),
        java.util.Map.entry("pot", "splash_potion"),
        java.util.Map.entry("pots", "splash_potion")
    );

    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        if (s.startsWith("minecraft:")) s = s.substring("minecraft:".length());
        // namespace:path → keep as path for vanilla, full for others
        if (s.contains(":")) {
            if (s.startsWith("minecraft:")) s = s.substring(10);
            else return s;
        }
        return ALIASES.getOrDefault(s, s);
    }

    public static Item resolve(String id) {
        String n = normalize(id);
        if (n == null) return null;
        String full = n.contains(":") ? n : "minecraft:" + n;
        try {
            Identifier ident = Identifier.tryParse(full);
            if (ident == null) return null;
            return Registries.ITEM.get(ident);
        } catch (Throwable t) {
            return null;
        }
    }

    public static List<String> parse(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String part : csv.split("[,;\\s]+")) {
            String n = normalize(part);
            if (n != null) uniq.add(n);
        }
        out.addAll(uniq);
        return out;
    }
}
