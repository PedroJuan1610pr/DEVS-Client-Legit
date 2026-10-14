package com.devs.client.module;

import com.devs.client.friends.FriendsManager;
import com.devs.client.render.HighlightMode;
import com.devs.client.render.HighlightRenderer;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Marcador visual solo en amigos visibles (sin ver a través de paredes). */
public final class FriendMarker extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final BooleanSetting box = add(new BooleanSetting("Box", true));
    public final BooleanSetting label = add(new BooleanSetting("Label", true));
    public final NumberSetting range = add(new NumberSetting("Range", 64, 16, 128, 8));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF55FF55));

    public FriendMarker() { super("Friend Marker", "Marks friends in view (not through walls).", "Render"); }

    @Override public void render() {
        if (mc.world == null || mc.player == null) return;
        FriendsManager.ensureLoaded();
        float tp = mc.getRenderTickCounter().getTickProgress(true);
        double r2 = range.get() * range.get();
        int col = color.get();
        boolean prev = HighlightRenderer.throughWalls;
        HighlightRenderer.throughWalls = false;
        try {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || !FriendsManager.isFriend(p)) continue;
                if (p.squaredDistanceTo(mc.player) > r2) continue;
                Vec3d lerped = p.getLerpedPos(tp);
                Box b = p.getBoundingBox().offset(lerped.subtract(p.getEntityPos()));
                if (box.get())
                    HighlightRenderer.drawBox(b, HighlightMode.OUTLINE, HighlightRenderer.withOpacity(col, 0.7f), 1.5f);
                if (label.get()) {
                    double dist = mc.player.distanceTo(p);
                    HighlightRenderer.label("★ " + p.getName().getString(),
                        new Vec3d(lerped.x, b.maxY + 0.35, lerped.z),
                        col | 0xFF000000, HighlightRenderer.labelScale(dist));
                }
            }
        } finally {
            HighlightRenderer.throughWalls = prev;
        }
    }
}
