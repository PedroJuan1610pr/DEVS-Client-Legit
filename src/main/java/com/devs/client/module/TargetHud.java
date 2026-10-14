package com.devs.client.module;

import com.devs.client.McCompat;

import com.devs.client.DevsClient;
import com.devs.client.friends.FriendsManager;
import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

/**
 * HUD estilo nametag del jugador (o entidad) al que miras.
 * Mantiene el target un tiempo configurable y hace fade in / fade out.
 */
public final class TargetHud extends Module {
    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final NumberSetting range = add(new NumberSetting("Range", 24, 4, 64, 1));
    public final NumberSetting holdTime = add(new NumberSetting("Hold Time (s)", 2.0, 0.3, 8.0, 0.1));
    public final NumberSetting fadeIn = add(new NumberSetting("Fade In (s)", 0.15, 0.0, 1.0, 0.05));
    public final NumberSetting fadeOut = add(new NumberSetting("Fade Out (s)", 0.35, 0.05, 1.5, 0.05));
    public final BooleanSetting playersOnly = add(new BooleanSetting("Players Only", true));
    public final BooleanSetting healthBar = add(new BooleanSetting("Health Bar", true));
    public final BooleanSetting armor = add(new BooleanSetting("Armor", true));
    public final BooleanSetting heldItem = add(new BooleanSetting("Held Item", true));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final BooleanSetting rounded = add(new BooleanSetting("Soft Edges", true));
    public final ColorSetting accent = add(new ColorSetting("Accent", 0xFF4DD2FF));

    private LivingEntity current;
    private LivingEntity display;       // last valid target we still show while fading
    private int ticksSinceSeen;         // ticks without a live look target
    private float alpha;                // 0..1 visual opacity
    private float healthDisplay = -1;   // smoothed health bar

    public TargetHud() {
        super("Target HUD", "Nametag-style HUD for the entity you look at, with fade in/out.", "Render");
    }

    @Override protected void onDisable() {
        current = null;
        display = null;
        alpha = 0;
        healthDisplay = -1;
        ticksSinceSeen = 0;
    }

    @Override public void tick() {
        if (!enabled() || mc.player == null || mc.world == null) {
            current = null;
            display = null;
            alpha = 0;
            return;
        }

        LivingEntity look = resolve();
        int holdTicks = Math.max(1, (int) (holdTime.get() * 20));

        if (look != null) {
            current = look;
            display = look;
            ticksSinceSeen = 0;
        } else {
            ticksSinceSeen++;
            if (ticksSinceSeen > holdTicks) {
                current = null;
                // keep display until fully faded
            }
        }

        // fade targets
        float inSpeed = fadeIn.get() <= 0 ? 1f : 1f / (float) (fadeIn.get() * 20);
        float outSpeed = fadeOut.get() <= 0 ? 1f : 1f / (float) (fadeOut.get() * 20);

        if (current != null) {
            alpha = Math.min(1f, alpha + inSpeed);
        } else {
            alpha = Math.max(0f, alpha - outSpeed);
            if (alpha <= 0.001f) {
                display = null;
                healthDisplay = -1;
            }
        }
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null || display == null || alpha <= 0.01f) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        if (display.isRemoved()) return;

        LivingEntity le = display;
        var tr = mc.textRenderer;

        String name = le.getName().getString();
        boolean friend = le instanceof PlayerEntity pl && FriendsManager.isFriend(pl);
        float hp = le.getHealth() + le.getAbsorptionAmount();
        float max = Math.max(1f, le.getMaxHealth());
        // smooth health bar
        if (healthDisplay < 0) healthDisplay = hp;
        healthDisplay += (hp - healthDisplay) * 0.25f;

        String hpLine = String.format("%.1f / %.0f", hp, max);
        String popsText = "";
        if (le instanceof PlayerEntity pl) {
            int n = DevsClient.TOTEM_POPS.popsOf(pl.getUuid());
            if (n > 0) popsText = n + " pop" + (n == 1 ? "" : "s");
        }
        int dist = (int) Math.round(mc.player.distanceTo(le));

        boolean showArmor = armor.get();
        boolean showHeld = heldItem.get();
        int gearSlots = 0;
        if (showArmor) {
            for (EquipmentSlot s : ARMOR) if (!McCompat.equipped(le, s).isEmpty()) gearSlots++;
        }
        if (showHeld && !le.getMainHandStack().isEmpty()) gearSlots++;

        int w = Math.max(130, tr.getWidth(name) + 48);
        int h = 30 + (healthBar.get() ? 6 : 0) + (gearSlots > 0 ? 20 : 0);
        int a = MathHelper.clamp((int) (alpha * 255), 0, 255);
        int aBg = MathHelper.clamp((int) (alpha * 0xC0), 0, 0xC0);

        HudLayoutManager.begin(ctx, HudLayoutManager.TARGET_HUD, w, h);

        int col = friend ? 0xFF55FF55 : (accent.get() | 0xFF000000);
        int colA = (col & 0x00FFFFFF) | (a << 24);
        int white = 0x00FFFFFF | (a << 24);
        int muted = 0x00AAAAAA | (a << 24);
        int gold = 0x00FFD94D | (a << 24);

        // background panel
        if (background.get()) {
            int bg = (aBg << 24) | 0x101018;
            if (rounded.get()) {
                // soft edge simulation: outer darker + inset
                ctx.fill(1, 0, w - 1, 1, bg);
                ctx.fill(0, 1, w, h - 1, bg);
                ctx.fill(1, h - 1, w - 1, h, bg);
            } else {
                ctx.fill(0, 0, w, h, bg);
            }
            // accent bar left
            ctx.fill(0, 0, 2, h, colA);
            // top accent line
            ctx.fill(2, 0, w, 1, (Math.min(a, 0x60) << 24) | (col & 0x00FFFFFF));
        }

        // name + distance
        ctx.drawTextWithShadow(tr, name, 6, 4, friend ? (0x0055FF55 | (a << 24)) : white);
        String distStr = dist + "m";
        ctx.drawTextWithShadow(tr, distStr, w - 6 - tr.getWidth(distStr), 4, muted);

        // hp text + pops
        ctx.drawTextWithShadow(tr, hpLine, 6, 14, colA);
        if (!popsText.isEmpty())
            ctx.drawTextWithShadow(tr, popsText, w - 6 - tr.getWidth(popsText), 14, gold);

        // health bar with smooth fill + absorption tint
        int barY = 24;
        if (healthBar.get()) {
            float ratio = MathHelper.clamp(healthDisplay / max, 0, 1);
            int barLeft = 6, barRight = w - 6, barH = 4;
            ctx.fill(barLeft, barY, barRight, barY + barH, (Math.min(a, 0x90) << 24) | 0x2A2A33);
            int fill = barLeft + Math.round((barRight - barLeft) * ratio);
            // gradient-ish: darker end
            ctx.fill(barLeft, barY, fill, barY + barH, colA);
            // shine line
            if (fill > barLeft + 2)
                ctx.fill(barLeft, barY, fill, barY + 1, (Math.min(a, 0x50) << 24) | 0xFFFFFF);
            barY += 6;
        }

        // armor + held item
        if (gearSlots > 0) {
            int x = 6, y = barY + 1;
            if (showArmor) {
                for (EquipmentSlot s : ARMOR) {
                    ItemStack st = McCompat.equipped(le, s);
                    if (st.isEmpty()) continue;
                    drawItemFaded(ctx, st, x, y, alpha);
                    x += 18;
                }
            }
            if (showHeld) {
                ItemStack main = le.getMainHandStack();
                if (!main.isEmpty()) drawItemFaded(ctx, main, x, y, alpha);
            }
        }

        HudLayoutManager.end(ctx);
    }

    private void drawItemFaded(DrawContext ctx, ItemStack st, int x, int y, float a) {
        // items don't support alpha easily; still draw — panel fade covers most of it
        if (a < 0.15f) return;
        ctx.drawItem(st, x, y);
        ctx.drawStackOverlay(mc.textRenderer, st, x, y);
    }

    private LivingEntity resolve() {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            var e = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (e instanceof LivingEntity le && e != mc.player
                && mc.player.distanceTo(le) <= range.get()) {
                if (playersOnly.get() && !(le instanceof PlayerEntity)) return null;
                return le;
            }
        }
        // fallback: nearest living roughly in look direction
        LivingEntity best = null;
        double bestDot = 0.93;
        var look = mc.player.getRotationVec(1f);
        for (var e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == mc.player || e.isRemoved()) continue;
            if (playersOnly.get() && !(le instanceof PlayerEntity)) continue;
            if (mc.player.distanceTo(le) > range.get()) continue;
            var to = le.getEyePos().subtract(mc.player.getEyePos()).normalize();
            double dot = to.dotProduct(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = le;
            }
        }
        return best;
    }
}
