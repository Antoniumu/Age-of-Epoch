package com.robertx22.age_of_exile.uncommon.utilityclasses;

import com.robertx22.age_of_exile.a_libraries.trinkets.MyTrinketUtils;
import com.robertx22.age_of_exile.a_libraries.trinkets.TrinketSlots;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.event_hooks.my_events.CollectGearEvent;
import com.robertx22.age_of_exile.saveclasses.unit.GearData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerUtils {

    public static List<Player> getNearbyPlayers(Level world, BlockPos pos, double range) {
        return world.getServer()
            .getPlayerList()
            .getPlayers()
            .stream()
            .filter(x -> pos.distToCenterSqr(x.getX(), x.getY(), x.getZ()) < range)
            .collect(Collectors.toList());

    }

    public static List<Player> getNearbyPlayers(Entity en, double range) {
        return getNearbyPlayers(en.level(), en.blockPosition(), range);
    }

    public static List<ItemStack> getEquippedStacksOf(Player player, BaseGearType type) {

        if (type.getVanillaSlotType() == null) {
            List<ItemStack> list = new ArrayList<>();
            if (type.gear_slot.equals(TrinketSlots.RING.name)) {
                list.addAll(MyTrinketUtils.getAllSlots(Arrays.asList(TrinketSlots.RING.name), player));
            }
            if (type.gear_slot.equals(TrinketSlots.NECKLACE.name)) {
                list.addAll(MyTrinketUtils.getAllSlots(Arrays.asList(TrinketSlots.NECKLACE.name), player));
            }
            return list;
        } else {
            return Arrays.asList(player.getItemBySlot(type.getVanillaSlotType()));
        }
    }

    public static ItemStack lowestDurabilityWornGear(Player player) {
        List<GearData> stacks = CollectGearEvent.getAllGear(null, player, Load.Unit(player));

        Optional<GearData> opt = stacks.stream()
            .filter(x -> !x.stack.isEmpty())
            .sorted(Comparator.comparingInt(x -> x.stack.getMaxDamage() - x.stack.getDamageValue()))
            .findFirst();

        if (opt.isPresent()) {
            return opt.get().stack;
        } else {
            return ItemStack.EMPTY;
        }

    }

    public static void giveItem(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty() || player == null) {
            return;
        }

        ItemStack remaining = stack.copy();

        // 1. If the player already holds matching items in OFFHAND (e.g. quiver arrows), top it up first
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()
            && offhand.getItem() == remaining.getItem()
            && offhand.getCount() < offhand.getMaxStackSize()
            && offhand.getComponents().equals(remaining.getComponents())) {
            int canAdd = Math.min(remaining.getCount(), offhand.getMaxStackSize() - offhand.getCount());
            offhand.grow(canAdd);
            remaining.shrink(canAdd);
        }

        // 2. Insert remaining items into main inventory
        if (!remaining.isEmpty()) {
            player.getInventory().add(remaining);
        }

        // 3. If all 36 main slots are occupied and OFFHAND is empty, place remainder into OFFHAND
        if (!remaining.isEmpty() && player.getOffhandItem().isEmpty()) {
            int canFit = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack toOffhand = remaining.copy();
            toOffhand.setCount(canFit);
            player.setItemSlot(EquipmentSlot.OFFHAND, toOffhand);
            remaining.shrink(canFit);
        }

        // 4. Fallback if all 37 slots are full: drop directly at player's feet with zero pickup delay
        if (!remaining.isEmpty() && !player.level().isClientSide()) {
            ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), remaining.copy());
            entity.setPickUpDelay(0);
            player.level().addFreshEntity(entity);
            remaining.setCount(0);
        }

        stack.setCount(0);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    public static Player nearestPlayer(ServerLevel world, LivingEntity entity) {
        return nearestPlayer(world, entity.position());
    }

    public static Player nearestPlayer(ServerLevel world, BlockPos pos) {
        return nearestPlayer(world, new Vec3(pos.getY(), pos.getY(), pos.getZ()));
    }

    public static Player nearestPlayer(ServerLevel world, Vec3 pos) {

        Optional<ServerPlayer> player = world.players()
            .stream()
            .min(Comparator.comparingDouble(x -> x.distanceToSqr(pos)));

        return player.orElse(null);
    }

}
