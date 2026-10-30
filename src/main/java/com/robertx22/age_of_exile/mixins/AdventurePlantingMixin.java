package com.robertx22.age_of_exile.mixins;
import com.robertx22.age_of_exile.airship.adventure.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public class AdventurePlantingMixin {
    @Inject(method="place",at=@At("RETURN"),require=1)
    private void aoe$planted(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> result){
        if(result.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer p && context.getLevel().getBlockState(context.getClickedPos()).is(BlockTags.SAPLINGS))
            AdventureService.event(p,AdventureCatalog.Metric.SAPLING,"sapling",1,p.level().dimension().identifier()+":"+context.getClickedPos().asLong());
    }
}
