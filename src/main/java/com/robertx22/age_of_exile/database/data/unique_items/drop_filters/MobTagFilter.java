package com.robertx22.age_of_exile.database.data.unique_items.drop_filters;

import com.robertx22.age_of_exile.loot.LootInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.List;

public class MobTagFilter extends DropFilter {

    public static String ID = "mob_tag";

    @Override
    public boolean canDrop(DropFilterData data, LootInfo info) {
        return info.mobKilled != null && info.mobKilled.getType()
            .builtInRegistryHolder()
            .is(TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(data.id)));
    }

    @Override
    public String GUID() {
        return ID;
    }

    public List<Component> getTooltip(DropFilterData data) {

        List<Component> list = new ArrayList<>();
        list.add(Component.literal("Существа с этим тегом могут оставить:"));
        list.add(Component.literal(data.id));
        return list;

    }
}
