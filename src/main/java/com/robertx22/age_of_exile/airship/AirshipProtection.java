package com.robertx22.age_of_exile.airship;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

/** Direct player actions. Explosion/piston/fluid and third-party tool coverage remains a separate phase. */
public final class AirshipProtection {
    private AirshipProtection() {}
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world,p,pos,state,be)->{
            if(!AirshipHub.protectedAt(world,pos)) return true;
            p.displayClientMessage(Component.literal("Общий корабль защищён: блоки нельзя ломать."),true);return false;
        });
        UseBlockCallback.EVENT.register((p,world,hand,hit)->{
            if(!AirshipHub.protectedAt(world,hit.getBlockPos()) && !AirshipHub.protectedAt(world,hit.getBlockPos().relative(hit.getDirection()))) return InteractionResult.PASS;
            var block=world.getBlockState(hit.getBlockPos()).getBlock();
            // Empty-handed doors/trapdoors remain usable; beds, containers, buckets and placed items do not.
            if(p.getItemInHand(hand).isEmpty() && (block instanceof DoorBlock || block instanceof TrapDoorBlock || block==Blocks.CRAFTING_TABLE)) return InteractionResult.PASS;
            p.displayClientMessage(Component.literal("Декор и хранилища общего хаба защищены."),true);return InteractionResult.FAIL;
        });
        AttackEntityCallback.EVENT.register((p,w,hand,e,hit)->AirshipHub.protectedAt(w,e.blockPosition())?InteractionResult.FAIL:InteractionResult.PASS);
        UseEntityCallback.EVENT.register((p,w,hand,e,hit)->{
            if(!AirshipHub.protectedAt(w,e.blockPosition()) || !AirshipService.npcId(e).isEmpty()) return InteractionResult.PASS;
            return InteractionResult.FAIL;
        });
    }
}
