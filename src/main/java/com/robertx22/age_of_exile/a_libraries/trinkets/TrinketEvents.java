package com.robertx22.age_of_exile.a_libraries.trinkets;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import dev.yumi.commons.TriState;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.event.TrinketEquipmentChangedCallback;
import eu.pb4.trinkets.api.event.TrinketModifySlotCountCallback;
import eu.pb4.trinkets.api.event.TrinketSlotCompatibilityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TrinketEvents {

    public static void reg() {

        // Fired whenever a trinket is equipped or unequipped -
        // replacement for the old CurioChangeEvent (Curios).
        TrinketEquipmentChangedCallback.EVENT.register(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "trinket_equipment_changed"),
            (previous, stack, slot, entity) -> {

                if (entity == null || entity.level().isClientSide()) {
                    return;
                }

                EntityData data = Load.Unit(entity);

                if (data != null) {
                    data.setEquipsChanged(true);
                    data.tryRecalculateStats();

                    if (entity instanceof ServerPlayer) {
                        data.syncToClient((ServerPlayer) entity);
                    }
                }

            });

        // Players get 2 ring slots (same as the old Curios "ring" size of 2)
        TrinketModifySlotCountCallback.EVENT.register(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "two_ring_slots"),
            (entity, slotType, inventory, count) -> {

                if (entity instanceof Player && slotType.getId()
                    .equals(TrinketSlots.RING.trinketId)) {
                    return 2;
                }

                return count;

            });

        // Let Age of Epoch jewelry items into their matching trinket slots.
        // Covers items implementing ITrinketType, gear items with a RING/NECKLACE gear slot,
        // and anything GearSlot recognizes as ring/necklace.
        TrinketSlotCompatibilityCallback.EVENT.register(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "trinket_slot_compatibility"),
            (stack, slot, entity, canEquipDefault) -> {

                if (stack == null || stack.isEmpty()) {
                    return TriState.DEFAULT;
                }

                Item item = stack.getItem();
                String slotId = slot.slotType().getId();
                String slotName = TrinketSlots.name(slotId);

                if (slotName == null) {
                    return TriState.DEFAULT;
                }

                if (item instanceof ITrinketType type
                    && type.trinketTypeName().equals(slotName)) {
                    return TriState.TRUE;
                }

                GearItemData gear = Gear.Load(stack);

                if (gear != null && gear.GetBaseGearType() != null
                    && gear.GetBaseGearType().getGearSlot() != null) {
                    if (slotName.equals(gear.GetBaseGearType().getGearSlot().GUID())) {
                        return TriState.TRUE;
                    }
                }

                GearSlot gearSlot = GearSlot.getSlotOf(item);

                if (gearSlot != null && slotName.equals(gearSlot.GUID())) {
                    return TriState.TRUE;
                }

                return TriState.DEFAULT;

            });

    }

}
