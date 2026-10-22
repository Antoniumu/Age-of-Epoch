package com.robertx22.mine_and_slash.loot.generators;


import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;
import net.minecraft.world.item.ItemStack;

public class MapLootGen extends BaseLootGen<MapBlueprint> {

    public MapLootGen(LootInfo info) {
        super(info);
    }

    @Override
    public float baseDropChance() {
        return ServerContainer.get().MAP_DROPRATE.get().floatValue();
    }

    @Override
    public LootType lootType() {
        return LootType.Map;
    }

    @Override
    public boolean condition() {
        if (ServerContainer.get().MIN_LEVEL_MAP_DROPS.get() >= info.level) {
            return false;
        }
        return true;
    }

    @Override
    public boolean hasLevelDistancePunishment() {
        return false;
    }

    @Override
    public ItemStack generateOne() {
        MapBlueprint blueprint = new MapBlueprint(info);

        return blueprint.createStack();
    }

}
