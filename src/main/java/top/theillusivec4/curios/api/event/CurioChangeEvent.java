package top.theillusivec4.curios.api.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class CurioChangeEvent extends Event {
    private final LivingEntity entityLiving;
    private final ItemStack from;
    private final ItemStack to;

    public CurioChangeEvent(LivingEntity entityLiving, ItemStack from, ItemStack to) {
        this.entityLiving = entityLiving;
        this.from = from;
        this.to = to;
    }

    public LivingEntity getEntityLiving() { return entityLiving; }

    public ItemStack getFrom() { return from; }

    public ItemStack getTo() { return to; }
}
