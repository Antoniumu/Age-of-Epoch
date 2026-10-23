package com.robertx22.age_of_exile.database.data.crafting_req;

import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class CraftingReq implements JsonExileRegistry<CraftingReq>, IAutoGson<CraftingReq> {

    public static CraftingReq SERIALIZER = new CraftingReq();

    public String item_id = "";
    public String skill = "";
    public int lvl = 1;

    public static CraftingReq of(Item item, PlayerSkillEnum skill, int lvl) {
        CraftingReq r = new CraftingReq();
        r.item_id = BuiltInRegistries.ITEM.getKey(item)
            .toString();
        r.lvl = lvl;
        r.skill = skill.id;

        r.addToSerializables();
        return r;
    }

    public boolean meets(Player player) {
        return Load.playerRPGData(player).professions
            .getDataFor(skill)
            .getLvl() >= lvl;
    }

    /**
     * Returns the requirement registered for that item, or null if it has none.
     * Uses the registry list on purpose: {@code ExileDB.ItemCraftReq().get(id)} would
     * log an error for every item that has no requirement.
     */
    public static CraftingReq getFor(Item item) {
        if (item == null || item == Items.AIR) {
            return null;
        }

        net.minecraft.resources.Identifier key = BuiltInRegistries.ITEM.getKey(item);

        if (key == null) {
            return null;
        }

        String id = key.toString();

        for (CraftingReq req : ExileDB.ItemCraftReq()
            .getList()) {
            if (req != null && id.equals(req.GUID())) {
                return req;
            }
        }
        return null;
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public Class<CraftingReq> getClassForSerialization() {
        return CraftingReq.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.CRAFTING_REQ;
    }

    @Override
    public String GUID() {
        return item_id;
    }
}
