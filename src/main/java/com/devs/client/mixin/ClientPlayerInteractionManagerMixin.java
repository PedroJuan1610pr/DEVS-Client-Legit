package com.devs.client.mixin;

import com.devs.client.DevsClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void devs$consumablePlace(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
                                      CallbackInfoReturnable<ActionResult> cir) {
        if (player == null) return;
        ActionResult r = cir.getReturnValue();
        if (r == null) return;
        if (!r.isAccepted()) return;
        ItemStack st = player.getStackInHand(hand);
        if (!st.isEmpty()) DevsClient.CONSUMABLE_COUNTER.onSelfBlockPlace(st.copy());
    }
}
