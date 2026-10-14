package com.devs.client;

import com.devs.client.gui.HudEditorScreen;
import com.devs.client.hud.HudLayoutManager;
import com.devs.client.profile.ProfileManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import com.devs.client.friends.FriendCommands;
import com.devs.client.net.DevsNetworking;
import com.devs.client.gui.ClickGuiScreen;
import com.devs.client.module.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class DevsClient implements ClientModInitializer {
    public static final ModuleManager MODULES = new ModuleManager();

    // Shared instances referenced by mixins
    public static final Fullbright FULLBRIGHT = new Fullbright();
    public static final Profiles PROFILES = new Profiles();
    public static final WeatherControl WEATHER = new WeatherControl();
    public static final HitLog HIT_LOG = new HitLog();
    public static final TotemPops TOTEM_POPS = new TotemPops();
    public static final ConsumableCounter CONSUMABLE_COUNTER = new ConsumableCounter();
    public static final DamageNumbers DAMAGE_NUMBERS = new DamageNumbers();
    public static final Notifications NOTIFICATIONS = new Notifications();
    public static final ChatUtils CHAT_UTILS = new ChatUtils();
    public static final FreeLook FREE_LOOK = new FreeLook();
    public static final NoRender NO_RENDER = new NoRender();
    public static final Viewmodel VIEWMODEL = new Viewmodel();
    public static final AutoReconnect AUTO_RECONNECT = new AutoReconnect();

    private static KeyBinding guiKey;
    private static KeyBinding hudKey;
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("devs", "main"));

    @Override public void onInitializeClient() {
        // ---- Render ----
        MODULES.register(FULLBRIGHT);
        MODULES.register(new Waypoints());
        MODULES.register(new MultiModule());
        MODULES.register(VIEWMODEL);
        MODULES.register(NO_RENDER);
        MODULES.register(FREE_LOOK);
        MODULES.register(new Nametags());
        MODULES.register(new Cosmetics());
        MODULES.register(new WaypointHud());
        MODULES.register(new Hitboxes());
        MODULES.register(new TargetHud());
        MODULES.register(new BlockOutline());
        MODULES.register(new FriendMarker());
        MODULES.register(new IdleAura());

        // ---- World ----
        MODULES.register(new ChunkBorders());
        MODULES.register(new LightLevel());
        MODULES.register(new PlacementPreview());
        MODULES.register(new SlimeChunks());

        // ---- Combat (info only) ----
        MODULES.register(HIT_LOG);
        MODULES.register(TOTEM_POPS);
        MODULES.register(CONSUMABLE_COUNTER);
        MODULES.register(DAMAGE_NUMBERS);
        MODULES.register(new ComboCounter());

        // ---- Misc / HUD ----
        MODULES.register(new HudInfo());
        MODULES.register(new SaturationOverlay());
        MODULES.register(new WorldClock());
        MODULES.register(new Speedometer());
        MODULES.register(new ReachDisplay());
        MODULES.register(new SessionStats());
        MODULES.register(new ElytraHud());
        MODULES.register(new PingGraph());
        MODULES.register(new TotemIndicator());
        MODULES.register(new HotbarWarning());
        MODULES.register(new KeybindList());
        MODULES.register(new CustomDisplay());
        MODULES.register(new ModuleDisplay());
        MODULES.register(new GuiThemeModule());
        MODULES.register(new DeathPoint());
        MODULES.register(new CopyCoords());
        MODULES.register(PROFILES);
        MODULES.register(WEATHER);
        MODULES.register(CHAT_UTILS);
        MODULES.register(new ChatMentions());
        MODULES.register(new ArmorHud());
        MODULES.register(new PotionHud());
        MODULES.register(NOTIFICATIONS);
        MODULES.register(new Friends());
        MODULES.register(new NetworkHud());
        MODULES.register(new BlockCounter());
        MODULES.register(new KeybindConflicts());
        MODULES.register(new HudPresets());
        MODULES.register(AUTO_RECONNECT);
        MODULES.register(new Panic());
        MODULES.register(new ScreenshotClean());
        MODULES.register(new ImageDisplay());

        DevsNetworking.registerClient();
        ProfileManager.load("auto");
        HudLayoutManager.load();
        for (var m : MODULES.all()) if (m instanceof GuiThemeModule g) g.apply();
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> {
            if (PROFILES.autoSave.get()) ProfileManager.save("auto");
            HudLayoutManager.save();
        });

        guiKey = KeyMappingHelper.registerKeyMapping(new KeyBinding(
            "key.devs.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        hudKey = KeyMappingHelper.registerKeyMapping(new KeyBinding(
            "key.devs.hudeditor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));

        // Comandos .friend (no se envían al servidor)
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (com.devs.client.multi.MultiCommands.handle(message)) return false;
            return !FriendCommands.handleChat(message);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) client.setScreen(new ClickGuiScreen());
            while (hudKey.wasPressed()) client.setScreen(new HudEditorScreen());
            for (var m : MODULES.all()) m.tick();
            FriendCommands.tick(client);
            DevsNetworking.clientTick();
            pollModuleKeybinds(client);
        });
        HudElementRegistry.addLast(Identifier.of("devs", "hud"), (ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (mc.options.hudHidden) return;
            if (ScreenshotClean.shouldHideHud()) return;
            for (var m : MODULES.all()) if (m.enabled()) m.renderHud(ctx);
        });
    }

    public static void renderFinders() {
        for (var m : MODULES.all()) if (m.enabled()) m.render();
    }

    public static MinecraftClient client() { return MinecraftClient.getInstance(); }

    private static void pollModuleKeybinds(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        if (client.currentScreen != null) return;
        long handle = client.getWindow().getHandle();
        for (var m : MODULES.all()) {
            int k = m.keybind();
            if (k < 0) continue;
            if (org.lwjgl.glfw.GLFW.glfwGetKey(handle, k) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                if (KeyEdge.justPressed(k)) m.toggle();
            } else {
                KeyEdge.released(k);
            }
        }
    }

    private static final class KeyEdge {
        private static final java.util.Set<Integer> down = new java.util.HashSet<>();
        static boolean justPressed(int k) {
            if (down.contains(k)) return false;
            down.add(k);
            return true;
        }
        static void released(int k) { down.remove(k); }
    }
}
