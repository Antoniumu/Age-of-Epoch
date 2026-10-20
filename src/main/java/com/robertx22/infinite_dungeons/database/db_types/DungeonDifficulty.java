package com.robertx22.infinite_dungeons.database.db_types;
import net.minecraft.network.chat.MutableComponent;

import com.robertx22.infinite_dungeons.db_init.RegistryTypes;
import com.robertx22.infinite_dungeons.exile_events.DungeonDifficultyTooltipEvent;
import com.robertx22.infinite_dungeons.exile_events.IDExileEvents;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.infinite_dungeons.util.ClientOnly;
import com.robertx22.infinite_dungeons.util.FormatUtils;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Mob;
public class DungeonDifficulty implements JsonExileRegistry<DungeonDifficulty>, IAutoGson<DungeonDifficulty> {
    public static DungeonDifficulty SERIALIZER = new DungeonDifficulty();

    public int weight = 1000;
    public String id = "";
    public String format = "";
    public int tier = 0;

    public float mns_lvl_req = 0F;
    public int mns_ilvl_req = 0;

    public float mob_hp_multi = 1;
    public float mob_dmg_multi = 1;
    public float coin_drop_multi = 1;

    public ChatFormatting getFormat() {
        return FormatUtils.of(format);
    }

    public MutableComponent getTranslatable() {
        return Component.translatable(MainID.MODID + ".difficulty." + id);
    }

    public Identifier getIconLoc() {
        return Identifier.fromNamespaceAndPath(MainID.MODID, "textures/gui/difficulty/" + id + ".png");
    }

    public List<Component> getTooltip() {

        List<Component> list = new ArrayList<>();
        list.add(getTranslatable());

        list.add(Component.literal("Mob HP: " + mob_hp_multi + "x"));
        list.add(Component.literal("Mob DMG: " + mob_dmg_multi + "x"));
        list.add(Component.literal("Coin Drops: " + coin_drop_multi + "x"));

        DungeonDifficultyTooltipEvent event = new DungeonDifficultyTooltipEvent(ClientOnly.getPlayer(), this, list);
        IDExileEvents.DIFFICULTY_TOOLTIP.callEvents(event);

        return list;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.DUNGEON_DIFFICULTY;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public Class<DungeonDifficulty> getClassForSerialization() {
        return DungeonDifficulty.class;
    }

}
