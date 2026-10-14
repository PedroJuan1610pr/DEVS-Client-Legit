package com.devs.client.mixin;

import com.devs.client.DevsClient;
import com.devs.client.hud.HudLayoutManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void devs$scoreboardPre(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (DevsClient.NO_RENDER != null && DevsClient.NO_RENDER.hideScoreboard()) {
            ci.cancel();
            return;
        }
        HudLayoutManager.pushScoreboard(context, objective);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("RETURN"), require = 0)
    private void devs$scoreboardPost(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        HudLayoutManager.popScoreboard(context);
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void devs$noCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (DevsClient.NO_RENDER != null && DevsClient.NO_RENDER.hideCrosshair()) ci.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void devs$noPotions(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (DevsClient.NO_RENDER != null && DevsClient.NO_RENDER.hidePotionIcons()) ci.cancel();
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true, require = 0)
    private void devs$noTitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (DevsClient.NO_RENDER != null && DevsClient.NO_RENDER.hideTitle()) ci.cancel();
    }

    /** AppleSkin-style saturation overlay after vanilla food icons. */
    @Inject(method = "renderFood", at = @At("RETURN"), require = 0)
    private void devs$saturationOverlay(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        try {
            if (com.devs.client.module.SaturationOverlay.INSTANCE != null)
                com.devs.client.module.SaturationOverlay.INSTANCE.renderOnFoodBar(context, player, top, right);
        } catch (Throwable ignored) {}
    }
}
