package top.theillusivec4.curios.api.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class CurioChangeEvent extends Event {
    private final LivingEntity entity;
    private final String identifier;
    private final int slotIndex;
    private final ItemStack from;
    private final ItemStack to;

    public CurioChangeEvent(LivingEntity entity, String identifier, int slotIndex, ItemStack from, ItemStack to) {
        this.entity = entity;
        this.identifier = identifier;
        this.slotIndex = slotIndex;
        this.from = from;
        this.to = to;
    }

    public LivingEntity getEntity() { return entity; }
    public String getIdentifier() { return identifier; }
    public int getSlotIndex() { return slotIndex; }
    public ItemStack getFrom() { return from; }
    public ItemStack getTo() { return to; }
}
