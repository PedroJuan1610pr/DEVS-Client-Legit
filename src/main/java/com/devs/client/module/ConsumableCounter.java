package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Contador unificado: gaps, pots (por tipo), perlas, bloques…
 * HUD You / Enemy. Picker de items en la GUI.
 */
public final class ConsumableCounter extends Module {
    private record Popup(Vec3d pos, String text, int born, int color) {}
    private record Eating(ItemStack stack, int ticks) {}
    private record HandSnap(Item mainItem, int mainCount, Item offItem, int offCount) {}

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final NumberSetting range = add(new NumberSetting("Range", 48, 8, 128, 4));
    public final ItemListSetting items = add(new ItemListSetting(
        "Items",
        "golden_apple,enchanted_golden_apple,splash_potion,lingering_potion,experience_bottle,obsidian,ender_pearl,end_crystal,totem_of_undying,chorus_fruit"));
    public final ModeSetting display = add(new ModeSetting("Display", "Icons", "Text", "Icons"));
    public final BooleanSetting hudSelf = add(new BooleanSetting("HUD Self", true));
    public final BooleanSetting hudEnemy = add(new BooleanSetting("HUD Enemy", true));
    public final BooleanSetting chat = add(new BooleanSetting("Chat", false));
    public final BooleanSetting floating = add(new BooleanSetting("Floating Text", true));
    public final BooleanSetting trackFood = add(new BooleanSetting("Track Eating", true));
    public final BooleanSetting trackPots = add(new BooleanSetting("Track Throws", true)); // pots, xp bottles, pearls…
    public final BooleanSetting trackHand = add(new BooleanSetting("Track Hand Drop", true));
    public final BooleanSetting trackPlace = add(new BooleanSetting("Track Block Place (Self)", true));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final NumberSetting duration = add(new NumberSetting("Duration (s)", 2.0, 0.5, 6.0, 0.5));
    public final NumberSetting minUseTicks = add(new NumberSetting("Min Use Ticks", 28, 5, 40, 1));
    public final ColorSetting selfColor = add(new ColorSetting("Self Color", 0xFF4DD2FF));
    public final ColorSetting enemyColor = add(new ColorSetting("Enemy Color", 0xFFFF4D6D));
    public final BooleanSetting resetBtn = add(new BooleanSetting("Clear Counters", false));
    public final BooleanSetting perPlayer = add(new BooleanSetting("Per Player HUD", true));
    public final BooleanSetting clearOnDeath = add(new BooleanSetting("Clear On Death", true));
    /** Activa esto, cierra la GUI y pulsa cualquier tecla para asignar el reset. Escape = quitar. */
    public final BooleanSetting bindResetKey = add(new BooleanSetting("Bind Reset Key", false));

    private final Map<UUID, Map<String, Integer>> counts = new HashMap<>();
    private int resetKeyCode = -1; // GLFW key, -1 = none
    private boolean resetKeyDown;
    private final Map<String, ItemStack> iconByKey = new HashMap<>();
    private final Map<UUID, Integer> lastSeen = new HashMap<>();
    private final Map<UUID, Eating> eating = new HashMap<>();
    private final Map<UUID, HandSnap> hands = new HashMap<>();
    private final Map<Item, Integer> prevSelfInv = new HashMap<>();
    private boolean selfInvReady;
    private final Set<Integer> seenPotions = new HashSet<>();
    private final List<Popup> popups = new ArrayList<>();
    private Set<Item> trackedCache = Set.of();
    private String trackedSrc = "";
    private int now;

    public ConsumableCounter() {
        super("Consumable Counter", "Count items used/thrown/placed. Pots by type. Dual HUD + item picker.", "Combat");
    }

    public int countOf(UUID id, String key) {
        if (!enabled()) return 0;
        Map<String, Integer> m = counts.get(id);
        return m == null ? 0 : m.getOrDefault(key, 0);
    }

    public Map<String, Integer> allOf(UUID id) {
        if (!enabled()) return Map.of();
        Map<String, Integer> m = counts.get(id);
        return m == null ? Map.of() : Map.copyOf(m);
    }

    public boolean iconsMode() { return "Icons".equals(display.get()); }

    @Override protected void onDisable() {
        counts.clear(); iconByKey.clear(); lastSeen.clear(); eating.clear();
        hands.clear(); prevSelfInv.clear(); selfInvReady = false;
        seenPotions.clear(); popups.clear();
    }

    private Set<Item> tracked() {
        String src = items.get() == null ? "" : items.get();
        if (src.equals(trackedSrc) && !trackedCache.isEmpty()) return trackedCache;
        trackedSrc = src;
        trackedCache = items.toItemSet();
        return trackedCache;
    }

    private static boolean isPotionItem(Item item) {
        return item == Items.SPLASH_POTION || item == Items.LINGERING_POTION || item == Items.POTION;
    }

    private static boolean isUseTracked(Item item) {
        ItemStack st = new ItemStack(item);
        return st.get(DataComponentTypes.FOOD) != null
            || st.get(DataComponentTypes.CONSUMABLE) != null;
    }

    private static boolean isThrownPotion(Item item) {
        return item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }

    /** Items que se tiran como entidad → solo se cuentan por ThrownItemEntity (evita x2). */
    private static boolean isThrowableTracked(Item item) {
        if (item == null) return false;
        if (isThrownPotion(item) || isPotionItem(item)) return true;
        return item == Items.EXPERIENCE_BOTTLE
            || item == Items.ENDER_PEARL
            || item == Items.SNOWBALL
            || item == Items.EGG
            || item == Items.ENDER_EYE
            || item == Items.SPLASH_POTION
            || item == Items.LINGERING_POTION;
    }

    private static String keyFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "unknown";
        Item item = stack.getItem();
        if (isPotionItem(item)) return potType(stack);
        Identifier id = Registries.ITEM.getId(item);
        return id == null ? "unknown" : id.getPath();
    }

    private static String potType(ItemStack stack) {
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null || !contents.hasEffects()) return "Water";
        StatusEffectInstance best = null;
        int bestPri = Integer.MIN_VALUE;
        for (StatusEffectInstance inst : contents.getEffects()) {
            int pri = potPriority(inst.getEffectType());
            if (pri > bestPri) { bestPri = pri; best = inst; }
        }
        if (best == null) return "Unknown";
        return potLabel(best.getEffectType());
    }

    private static int potPriority(RegistryEntry<StatusEffect> type) {
        if (type == StatusEffects.INSTANT_HEALTH) return 100;
        if (type == StatusEffects.STRENGTH) return 90;
        if (type == StatusEffects.SPEED) return 85;
        if (type == StatusEffects.FIRE_RESISTANCE) return 80;
        if (type == StatusEffects.INSTANT_DAMAGE) return 75;
        if (type == StatusEffects.POISON) return 70;
        if (type == StatusEffects.SLOWNESS) return 65;
        if (type == StatusEffects.WEAKNESS) return 60;
        if (type == StatusEffects.REGENERATION) return 55;
        if (type == StatusEffects.INVISIBILITY) return 50;
        if (type == StatusEffects.NIGHT_VISION) return 40;
        if (type == StatusEffects.SLOW_FALLING) return 35;
        if (type == StatusEffects.RESISTANCE) return 30;
        if (type == StatusEffects.JUMP_BOOST) return 25;
        if (type == StatusEffects.WATER_BREATHING) return 20;
        return 10;
    }

    private static String potLabel(RegistryEntry<StatusEffect> type) {
        if (type == StatusEffects.INSTANT_HEALTH) return "Health";
        if (type == StatusEffects.STRENGTH) return "Strength";
        if (type == StatusEffects.SPEED) return "Speed";
        if (type == StatusEffects.FIRE_RESISTANCE) return "Fire Res";
        if (type == StatusEffects.INSTANT_DAMAGE) return "Harm";
        if (type == StatusEffects.POISON) return "Poison";
        if (type == StatusEffects.SLOWNESS) return "Slow";
        if (type == StatusEffects.WEAKNESS) return "Weak";
        if (type == StatusEffects.REGENERATION) return "Regen";
        if (type == StatusEffects.INVISIBILITY) return "Invis";
        if (type == StatusEffects.NIGHT_VISION) return "Night Vis";
        if (type == StatusEffects.SLOW_FALLING) return "Slow Fall";
        if (type == StatusEffects.RESISTANCE) return "Resist";
        if (type == StatusEffects.JUMP_BOOST) return "Jump";
        if (type == StatusEffects.WATER_BREATHING) return "Water Br";
        try { return type.value().getName().getString(); }
        catch (Throwable t) { return "Other"; }
    }

    private boolean inRange(PlayerEntity p) {
        if (mc.player == null) return false;
        double r = range.get();
        return p.squaredDistanceTo(mc.player) <= r * r;
    }

    public void onSelfBlockPlace(ItemStack stack) {
        if (!enabled() || !trackPlace.get() || mc.player == null) return;
        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof BlockItem)) return;
        if (!tracked().contains(stack.getItem())) return;
        record(mc.player, stack, 1);
    }

    private void record(PlayerEntity player, ItemStack stack, int amount) {
        if (player == null || stack == null || stack.isEmpty() || amount <= 0) return;
        if (!inRange(player) && player != mc.player) return;
        String key = keyFor(stack);
        recordKey(player, key, stack, amount);
    }

    private void record(PlayerEntity player, Item item, int amount) {
        if (item == null) return;
        record(player, new ItemStack(item), amount);
    }

    private void recordKey(PlayerEntity player, String key, ItemStack icon, int amount) {
        if (player == null || key == null || amount <= 0) return;
        if (!inRange(player) && player != mc.player) return;
        UUID id = player.getUuid();
        Map<String, Integer> m = counts.computeIfAbsent(id, k -> new LinkedHashMap<>());
        int n = m.merge(key, amount, Integer::sum);
        lastSeen.put(id, now);
        if (icon != null && !icon.isEmpty()) iconByKey.put(key, icon.copy());
        String name = player.getName().getString();
        boolean self = player == mc.player;
        int col = (self ? selfColor.get() : enemyColor.get()) | 0xFF000000;
        String floatText = iconsMode() ? (key + " x" + n) : (name + " " + key + " x" + n);
        if (floating.get()) {
            Vec3d pos = new Vec3d(player.getX(), player.getBoundingBox().maxY + 0.9, player.getZ());
            popups.add(new Popup(pos, floatText, now, col));
        }
        if (chat.get() && mc.player != null) {
            mc.player.sendMessage(
                Text.literal("[DEVS] " + name + " used " + key + " (x" + n + ")")
                    .formatted(self ? Formatting.AQUA : Formatting.RED));
        }
    }

    /** Self: bajada de stacks en inventario (gaps, perlas, cristales…). */

    /** Si el throw no trae owner, atribuye al jugador cercano mas plausible. */
    private PlayerEntity nearestThrower(ThrownItemEntity thrown, double r2, Set<Item> track) {
        if (mc.world == null) return null;
        PlayerEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == null) continue;
            double d = pl.squaredDistanceTo(thrown);
            if (d > 36.0) continue; // ~6 bloques
            if (d >= bestD) continue;
            best = pl;
            bestD = d;
        }
        // solo si esta dentro del range del modulo respecto a ti (salvo self)
        if (best != null && mc.player != null
            && best != mc.player
            && best.squaredDistanceTo(mc.player) > r2) {
            return null;
        }
        return best;
    }

    private void trackSelfInventory(Set<Item> track) {
        if (mc.player == null) { prevSelfInv.clear(); selfInvReady = false; return; }
        // No contar si hay inventario/cofre abierto (mover items)
        if (mc.currentScreen != null
            && !(mc.currentScreen instanceof com.devs.client.gui.ClickGuiScreen)
            && !(mc.currentScreen instanceof com.devs.client.gui.ItemListEditorScreen)
            && !(mc.currentScreen instanceof com.devs.client.gui.HudEditorScreen)) {
            prevSelfInv.clear();
            selfInvReady = false;
            return;
        }
        Map<Item, Integer> cur = new HashMap<>();
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (st.isEmpty()) continue;
            Item it = st.getItem();
            if (!track.contains(it)) continue;
            // pots/xp/pearls → entidad tirada; comida → solo al TERMINAR de comer
            if (isThrowableTracked(it) || isUseTracked(it)) continue;
            if (it instanceof BlockItem && trackPlace.get()) continue;
            cur.merge(it, st.getCount(), Integer::sum);
        }
        if (!selfInvReady) {
            prevSelfInv.clear();
            prevSelfInv.putAll(cur);
            selfInvReady = true;
            return;
        }
        for (var e : prevSelfInv.entrySet()) {
            Item it = e.getKey();
            if (!track.contains(it)) continue;
            int was = e.getValue();
            int nowC = cur.getOrDefault(it, 0);
            if (nowC < was) {
                if (mc.player.isUsingItem() && !mc.player.getActiveItem().isEmpty()
                    && mc.player.getActiveItem().getItem() == it) continue;
                record(mc.player, new ItemStack(it), was - nowC);
            }
        }
        prevSelfInv.clear();
        prevSelfInv.putAll(cur);
    }

    @Override public void tick() {
        now++;
        if (!enabled()) return;

        if (resetBtn.get()) {
            clearAllCounters();
            resetBtn.set(false);
            if (mc.player != null)
                mc.player.sendMessage(Text.literal("[DEVS] Consumables cleared").formatted(Formatting.GRAY));
        }
        pollResetKeybind();

        int life = (int) (duration.get() * 20);
        popups.removeIf(p -> now - p.born() > life);

        if (mc.world == null || mc.player == null) {
            counts.clear(); lastSeen.clear(); eating.clear(); hands.clear();
            prevSelfInv.clear(); selfInvReady = false; seenPotions.clear();
            return;
        }

        Set<Item> track = tracked();
        if (track.isEmpty()) return;

        double r2 = range.get() * range.get();
        int minTicks = minUseTicks.get().intValue();

        // YOU: inventario
        trackSelfInventory(track);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.squaredDistanceTo(mc.player) > r2 && player != mc.player) {
                eating.remove(player.getUuid());
                hands.remove(player.getUuid());
                continue;
            }
            UUID id = player.getUuid();
            lastSeen.put(id, now);
            boolean isSelf = player == mc.player;

            // Comer: self + enemigos — SOLO si termino (ticks ~ maxUseTime), no al cancelar
            if (trackFood.get()) {
                if (player.isUsingItem()) {
                    ItemStack active = player.getActiveItem();
                    if (!active.isEmpty() && track.contains(active.getItem()) && isUseTracked(active.getItem())) {
                        int used = Math.max(1, player.getItemUseTime());
                        eating.put(id, new Eating(active.copy(), used));
                    }
                } else {
                    Eating was = eating.remove(id);
                    if (was != null && !was.stack().isEmpty()) {
                        int maxUse;
                        try {
                            maxUse = was.stack().getMaxUseTime(player);
                        } catch (Throwable t) {
                            maxUse = 32; // golden apple default
                        }
                        if (maxUse <= 0) maxUse = 32;
                        // debe llegar al menos al 85% del tiempo de uso (no cuenta si cancela)
                        int need = Math.max(minTicks, (int) Math.ceil(maxUse * 0.85));
                        if (was.ticks() >= need) {
                            record(player, was.stack(), 1);
                        }
                    }
                }
            }

            // Hand drop: solo enemigos
            if (trackHand.get() && !isSelf) {
                ItemStack main = player.getMainHandStack();
                ItemStack off = player.getOffHandStack();
                HandSnap prev = hands.get(id);
                HandSnap cur = new HandSnap(
                    main.isEmpty() ? null : main.getItem(),
                    main.isEmpty() ? 0 : main.getCount(),
                    off.isEmpty() ? null : off.getItem(),
                    off.isEmpty() ? 0 : off.getCount());
                if (prev != null) {
                    if (prev.mainItem() != null && track.contains(prev.mainItem())
                        && !isUseTracked(prev.mainItem()) && !isThrowableTracked(prev.mainItem())
                        && prev.mainItem() == cur.mainItem()
                        && cur.mainCount() < prev.mainCount()
                        && !player.isUsingItem()) {
                        record(player, prev.mainItem(), prev.mainCount() - cur.mainCount());
                    }
                    if (prev.offItem() != null && track.contains(prev.offItem())
                        && !isUseTracked(prev.offItem()) && !isThrowableTracked(prev.offItem())
                        && prev.offItem() == cur.offItem()
                        && cur.offCount() < prev.offCount()
                        && !player.isUsingItem()) {
                        record(player, prev.offItem(), prev.offCount() - cur.offCount());
                    }
                }
                hands.put(id, cur);
            }
        }

        // Throws: splash pots (por tipo), xp bottles, pearls, snowballs… (ThrownItemEntity)
        if (trackPots.get()) {
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof ThrownItemEntity thrown)) continue;
                int eid = e.getId();
                if (seenPotions.contains(eid)) continue;
                Entity owner = thrown.getOwner();
                PlayerEntity player = null;
                if (owner instanceof PlayerEntity pe) {
                    player = pe;
                } else {
                    // algunos servidores no setean owner al instante → jugador mas cercano que lo tenga trackeado
                    player = nearestThrower(thrown, r2, track);
                }
                if (player == null) continue;
                if (player.squaredDistanceTo(mc.player) > r2 && player != mc.player) continue;
                ItemStack st = thrown.getStack();
                if (st == null || st.isEmpty()) {
                    // fallback: xp bottle entity a veces sin stack visible
                    if (e.getClass().getSimpleName().toLowerCase().contains("experience")) {
                        st = new ItemStack(Items.EXPERIENCE_BOTTLE);
                    } else continue;
                }
                if (!track.contains(st.getItem())) continue;
                seenPotions.add(eid);
                record(player, st, 1);
            }
            if ((now & 31) == 0) {
                seenPotions.removeIf(id -> mc.world.getEntityById(id) == null);
            }
        }

        if (clearOnDeath.get() && mc.world != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (player.isAlive() && player.getHealth() > 0f) continue;
                UUID id = player.getUuid();
                if (counts.remove(id) != null) {
                    eating.remove(id);
                    hands.remove(id);
                }
            }
        }

        Iterator<Map.Entry<UUID, Map<String, Integer>>> it = counts.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            UUID id = en.getKey();
            if (mc.world.getPlayerByUuid(id) != null) continue;
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(id) != null) {
                if (now - lastSeen.getOrDefault(id, now) < 20 * 60) continue;
            }
            lastSeen.remove(id);
            eating.remove(id);
            hands.remove(id);
            it.remove();
        }
    }

    @Override public void render() {
        if (!floating.get() || popups.isEmpty() || mc.player == null) return;
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        double life = Math.max(1, duration.get() * 20);
        for (Popup p : popups) {
            double age = (now - p.born()) + tp;
            if (age > life) continue;
            float fade = (float) (1.0 - age / life);
            Vec3d pos = p.pos().add(0, age * 0.03, 0);
            double dist = mc.player.getEyePos().distanceTo(pos);
            HighlightRenderer.label(p.text(), pos,
                HighlightRenderer.withOpacity(p.color(), Math.max(0.15f, fade)),
                HighlightRenderer.labelScale(dist));
        }
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;

        UUID selfId = mc.player.getUuid();
        Map<String, Integer> selfMap = counts.getOrDefault(selfId, Map.of());

        if (hudSelf.get()) {
            drawPanel(ctx, HudLayoutManager.CONSUMABLE_SELF, "You", selfMap, selfColor.get() | 0xFF000000);
        }
        if (!hudEnemy.get()) return;

        int accent = enemyColor.get() | 0xFF000000;
        if (!perPlayer.get()) {
            Map<String, Integer> enemyMap = new LinkedHashMap<>();
            for (var en : counts.entrySet()) {
                if (en.getKey().equals(selfId)) continue;
                PlayerEntity pe = mc.world != null ? mc.world.getPlayerByUuid(en.getKey()) : null;
                if (pe != null && (!pe.isAlive() || pe.getHealth() <= 0f)) continue;
                for (var e : en.getValue().entrySet()) {
                    enemyMap.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
            drawPanel(ctx, HudLayoutManager.CONSUMABLE_ENEMY, "Enemy", enemyMap, accent);
            return;
        }

        List<UUID> order = new ArrayList<>();
        for (var en : counts.entrySet()) {
            if (en.getKey().equals(selfId)) continue;
            if (en.getValue() == null || en.getValue().isEmpty()) continue;
            PlayerEntity pe = mc.world != null ? mc.world.getPlayerByUuid(en.getKey()) : null;
            if (pe != null && (!pe.isAlive() || pe.getHealth() <= 0f)) continue;
            order.add(en.getKey());
        }
        order.sort((a, b) -> Integer.compare(lastSeen.getOrDefault(b, 0), lastSeen.getOrDefault(a, 0)));

        int offsetY = 0;
        for (UUID id : order) {
            Map<String, Integer> map = counts.getOrDefault(id, Map.of());
            if (map.isEmpty()) continue;
            String name = nameOf(id);
            int h = panelHeight(map);
            drawPanelOffset(ctx, HudLayoutManager.CONSUMABLE_ENEMY, name, map, accent, 0, offsetY);
            offsetY += h + 4;
        }
    }

    private String nameOf(UUID id) {
        if (mc.world != null) {
            PlayerEntity pe = mc.world.getPlayerByUuid(id);
            if (pe != null) return pe.getName().getString();
        }
        if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(id);
            if (entry != null) return entry.getProfile().name();
        }
        return id.toString().substring(0, 8);
    }

    private int panelHeight(Map<String, Integer> map) {
        int rowH = iconsMode() ? 18 : 12;
        return 12 + Math.max(1, map.size()) * rowH + 6;
    }

    private void clearAllCounters() {
        counts.clear(); iconByKey.clear(); lastSeen.clear(); eating.clear();
        hands.clear(); prevSelfInv.clear(); selfInvReady = false;
        seenPotions.clear(); popups.clear();
    }

    private void pollResetKeybind() {
        if (mc.getWindow() == null) return;
        long handle = mc.getWindow().getHandle();

        // Captura de tecla: Bind Reset Key = ON, sin pantalla abierta
        if (bindResetKey.get()) {
            if (mc.currentScreen != null) return;
            for (int k = 32; k <= 348; k++) {
                if (org.lwjgl.glfw.GLFW.glfwGetKey(handle, k) != org.lwjgl.glfw.GLFW.GLFW_PRESS) continue;
                if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                    resetKeyCode = -1;
                    bindResetKey.set(false);
                    if (mc.player != null)
                        mc.player.sendMessage(Text.literal("[DEVS] Reset keybind cleared").formatted(Formatting.GRAY));
                    return;
                }
                resetKeyCode = k;
                bindResetKey.set(false);
                String name = com.devs.client.gui.BindUtil.name(k);
                if (mc.player != null)
                    mc.player.sendMessage(Text.literal("[DEVS] Reset keybind: " + name).formatted(Formatting.GREEN));
                return;
            }
            return;
        }

        if (resetKeyCode < 0) { resetKeyDown = false; return; }
        if (mc.currentScreen != null) { resetKeyDown = false; return; }
        boolean down = org.lwjgl.glfw.GLFW.glfwGetKey(handle, resetKeyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (down && !resetKeyDown) {
            clearAllCounters();
            if (mc.player != null)
                mc.player.sendMessage(Text.literal("[DEVS] Consumables cleared").formatted(Formatting.GRAY));
        }
        resetKeyDown = down;
    }

    private void drawPanel(DrawContext ctx, String layoutId, String title, Map<String, Integer> map, int accent) {
        var tr = mc.textRenderer;
        boolean icons = iconsMode();
        List<Map.Entry<String, Integer>> rows = new ArrayList<>(map.entrySet());
        rows.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int rowH = icons ? 18 : 12;
        int titleH = 12;
        int contentH = titleH + Math.max(1, rows.size()) * rowH + 6;
        int maxW = Math.max(tr.getWidth(title), tr.getWidth("(none)"));
        for (var e : rows) {
            if (icons) maxW = Math.max(maxW, 16 + 4 + tr.getWidth(String.valueOf(e.getValue())));
            else maxW = Math.max(maxW, tr.getWidth(e.getKey() + " x" + e.getValue()));
        }
        int contentW = maxW + 12;

        HudLayoutManager.begin(ctx, layoutId, contentW, contentH);
        int x = 3, y = 3;
        if (background.get()) {
            ctx.fill(x - 3, y - 3, x + contentW - 3, y + contentH - 3, 0x90000000);
            ctx.fill(x - 3, y - 3, x - 1, y + contentH - 3, accent);
        }
        ctx.drawTextWithShadow(tr, title, x, y, accent);
        y += titleH;
        if (rows.isEmpty()) {
            ctx.drawTextWithShadow(tr, "(none)", x, y + 1, 0xFF888888);
            HudLayoutManager.end(ctx);
            return;
        }
        for (var e : rows) {
            if (icons) {
                ItemStack icon = iconFor(e.getKey());
                ctx.drawItem(icon, x, y);
                ctx.drawTextWithShadow(tr, String.valueOf(e.getValue()), x + 18, y + 4, 0xFFFFFFFF);
            } else {
                ctx.drawTextWithShadow(tr, e.getKey() + " x" + e.getValue(), x, y + 1, 0xFFFFFFFF);
            }
            y += rowH;
        }
        HudLayoutManager.end(ctx);
    }

    private void drawPanelOffset(DrawContext ctx, String layoutId, String title, Map<String, Integer> map, int accent, int offX, int offY) {
        var tr = mc.textRenderer;
        boolean icons = iconsMode();
        List<Map.Entry<String, Integer>> rows = new ArrayList<>(map.entrySet());
        rows.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int rowH = icons ? 18 : 12;
        int titleH = 12;
        int contentH = titleH + Math.max(1, rows.size()) * rowH + 6;
        int maxW = Math.max(tr.getWidth(title), tr.getWidth("(none)"));
        for (var e : rows) {
            if (icons) maxW = Math.max(maxW, 16 + 4 + tr.getWidth(String.valueOf(e.getValue())));
            else maxW = Math.max(maxW, tr.getWidth(e.getKey() + " x" + e.getValue()));
        }
        int contentW = maxW + 12;

        HudLayoutManager.begin(ctx, layoutId, contentW, contentH + offY);
        // offset manual dentro del layout
        int x = 3 + offX, y = 3 + offY;
        if (background.get()) {
            ctx.fill(x - 3, y - 3, x + contentW - 3, y + contentH - 3, 0x90000000);
            ctx.fill(x - 3, y - 3, x - 1, y + contentH - 3, accent);
        }
        ctx.drawTextWithShadow(tr, title, x, y, accent);
        y += titleH;
        if (rows.isEmpty()) {
            ctx.drawTextWithShadow(tr, "(none)", x, y + 1, 0xFF888888);
        } else {
            for (var e : rows) {
                if (icons) {
                    ItemStack icon = iconFor(e.getKey());
                    ctx.drawItem(icon, x, y);
                    ctx.drawTextWithShadow(tr, String.valueOf(e.getValue()), x + 18, y + 4, 0xFFFFFFFF);
                } else {
                    ctx.drawTextWithShadow(tr, e.getKey() + " x" + e.getValue(), x, y + 1, 0xFFFFFFFF);
                }
                y += rowH;
            }
        }
        HudLayoutManager.end(ctx);
    }

    private ItemStack iconFor(String key) {
        ItemStack cached = iconByKey.get(key);
        if (cached != null && !cached.isEmpty()) return cached;
        return switch (key) {
            case "Health" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.STRONG_HEALING);
            case "Strength" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.STRENGTH);
            case "Speed" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.SWIFTNESS);
            case "Fire Res" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.FIRE_RESISTANCE);
            case "Harm" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.HARMING);
            case "Poison" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.POISON);
            case "Slow" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.SLOWNESS);
            case "Weak" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.WEAKNESS);
            case "Regen" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.REGENERATION);
            case "Invis" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.INVISIBILITY);
            case "Night Vis" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.NIGHT_VISION);
            case "Slow Fall" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.SLOW_FALLING);
            case "Jump" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.LEAPING);
            case "Water Br" -> PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.WATER_BREATHING);
            case "Water" -> new ItemStack(Items.SPLASH_POTION);
            default -> {
                Item it = ItemListSetting.resolve(key);
                yield it != null ? new ItemStack(it) : new ItemStack(Items.BARRIER);
            }
        };
    }
}
