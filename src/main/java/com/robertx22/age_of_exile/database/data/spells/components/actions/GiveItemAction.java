package com.robertx22.age_of_exile.database.data.spells.components.actions;

import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Collection;

import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.COUNT;
import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.ITEM;

public class GiveItemAction extends SpellAction {

    public GiveItemAction() {
        super(Arrays.asList(ITEM, COUNT));
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        try {
            if (ctx.world.isClientSide()) {
                return;
            }
            if (!(ctx.caster instanceof Player player)) {
                return;
            }

            String itemId = data.get(ITEM);
            int totalCount = data.get(COUNT).intValue();
            if (totalCount <= 0) {
                return;
            }

            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
            if (item == null || item == Items.AIR) {
                return;
            }

            int maxStack = new ItemStack(item).getMaxStackSize();
            int remaining = totalCount;
            while (remaining > 0) {
                int batch = Math.min(remaining, maxStack);
                PlayerUtils.giveItem(new ItemStack(item, batch), player);
                remaining -= batch;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MapHolder create(Item item, Double count) {
        MapHolder d = new MapHolder();
        d.type = GUID();
        d.put(ITEM, BuiltInRegistries.ITEM.getKey(item).toString());
        d.put(COUNT, count);
        return d;
    }

    @Override
    public String GUID() {
        return "give_item";
    }
}
