package com.robertx22.age_of_exile.database.data.crafting_req;

import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
