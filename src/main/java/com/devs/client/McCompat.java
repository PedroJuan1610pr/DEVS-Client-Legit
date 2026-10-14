package com.devs.client;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

/**
 * Compat 26.1.2 + Modern Yarn (nombres reales del mappings.tiny).
 *
 * LivingEntity.getEquipment(EquipmentSlot)  — antes getEquippedStack / getItemBySlot
 * ChunkPos.x() / z()                        — record components
 * World.getOverworldTime()                  — reloj del mundo
 */
public final class McCompat {
    private McCompat() {}

    public static int chunkX(ChunkPos cp) {
        return cp.x();
    }

    public static int chunkZ(ChunkPos cp) {
        return cp.z();
    }

    public static long dayTime(World world) {
        return world.getOverworldTime();
    }

    public static ItemStack equipped(LivingEntity e, EquipmentSlot slot) {
        if (e == null || slot == null) return ItemStack.EMPTY;
        ItemStack st = e.getEquipment(slot);
        return st == null ? ItemStack.EMPTY : st;
    }
}
