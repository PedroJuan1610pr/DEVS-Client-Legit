package com.devs.client.module;

import com.devs.client.setting.BooleanSetting;
import com.devs.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

/** Mira alrededor sin girar al jugador (Autism FreeLook). Hold-to-activate no; toggle. */
public final class FreeLook extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final ModeSetting perspective = add(new ModeSetting("Perspective", "Back", "Back", "Front"));
    public final BooleanSetting noPitchLimit = add(new BooleanSetting("No Pitch Limit", false));

    public float cameraYaw, cameraPitch;
    private Perspective previous;

    public FreeLook() { super("FreeLook", "Look around without rotating your character.", "Render"); }

    @Override protected void onEnable() {
        if (mc.player != null) {
            cameraYaw = mc.player.getYaw();
            cameraPitch = mc.player.getPitch();
        }
        if (mc.options != null) {
            previous = mc.options.getPerspective();
            mc.options.setPerspective("Front".equals(perspective.get())
                ? Perspective.THIRD_PERSON_FRONT : Perspective.THIRD_PERSON_BACK);
        }
    }

    @Override protected void onDisable() {
        if (mc.options != null && previous != null) mc.options.setPerspective(previous);
        previous = null;
    }

    @Override public void tick() {
        if (!enabled() || mc.options == null) return;
        Perspective want = "Front".equals(perspective.get())
            ? Perspective.THIRD_PERSON_FRONT : Perspective.THIRD_PERSON_BACK;
        if (mc.options.getPerspective() != want) mc.options.setPerspective(want);
    }

    /** Llamado desde mixin de raton: devuelve true si consume el look del jugador. */
    public boolean onMouse(double dx, double dy) {
        if (!enabled()) return false;
        cameraYaw += (float) (dx * 0.15);
        cameraPitch += (float) (dy * 0.15);
        if (!noPitchLimit.get()) cameraPitch = MathHelper.clamp(cameraPitch, -90f, 90f);
        return true;
    }
}
