package com.robertx22.infinite_dungeons.database.db_types.layout;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.database.CompletitionScore;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.DungeonType;
import com.robertx22.infinite_dungeons.database.db_types.ShopList;
import com.robertx22.infinite_dungeons.database.db_types.group.DungeonGroup;
import com.robertx22.infinite_dungeons.database.db_types.layout.config.CleanUpDungeonConfig;
import com.robertx22.infinite_dungeons.database.db_types.layout.config.SlaughterDungeonConfig;
import com.robertx22.infinite_dungeons.database.db_types.layout.config.WaveBasedDungeonConfig;
import com.robertx22.infinite_dungeons.database.ids.DungeonGroupIds;
import com.robertx22.infinite_dungeons.database.ids.DungeonTypeIds;
import com.robertx22.infinite_dungeons.db_init.RegistryTypes;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.infinite_dungeons.util.FormatUtils;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class DungeonLayout implements JsonExileRegistry<DungeonLayout>, IAutoGson<DungeonLayout> {
    public static DungeonLayout SERIALIZER = new DungeonLayout();

    public int weight = 1000;
    public String id = "";
    public String format = "";

    public String dungeon_type = DungeonTypeIds.WAVE;
    public String dungeon_group = DungeonGroupIds.SKELETAL;

    public String structure_file = "";

    public List<DungeonSpawn> spawns = new ArrayList<>();

    // it matches based on priority of shop lists so there's no need for datapack overriding priority trouble
    public ShopList getShopList() {
        List<ShopList> list = DungeonsDB.ShopLists()
            .getFiltered(x -> x.for_layout_id.equals(GUID()));
        list.sort(Comparator.comparing(o -> -o.priority));

        return list.get(0);
    }

    public HashMap<CompletitionScore, ScoreConfig> score_configs = new HashMap<>();

    public SlaughterDungeonConfig slaughter_config = null;
    public WaveBasedDungeonConfig wave_config = null;
    public CleanUpDungeonConfig clean_up_config = null;

    public ResourceLocation getIconTextureLoc() {
        return ResourceLocation.fromNamespaceAndPath(MainID.MODID, "textures/gui/dungeon_icon/" + id + ".png");
    }

    public CompletitionScore getScore(ControlBlockEntity ce) {
        int sec = ce.data.ticks / 20;

        if (sec < score_configs.get(CompletitionScore.GOLD).reward_time_seconds) {
            return CompletitionScore.GOLD;
        }
        if (sec < score_configs.get(CompletitionScore.SILVER).reward_time_seconds) {
            return CompletitionScore.SILVER;
        }
        return CompletitionScore.BRONZE;
    }

    public DungeonType getDungeonType() {
        return DungeonsDB.DungeonTypes()
            .get(dungeon_type);
    }

    public DungeonGroup getGroup() {
        return DungeonsDB.Groups()
            .get(dungeon_group);
    }

    public ChatFormatting getFormat() {
        return FormatUtils.of(format);
    }

    public Component getTranslatable() {
        return Component.translatable(MainID.MODID + ".layout." + id);
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.DUNGEON_LAYOUT;
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
    public Class<DungeonLayout> getClassForSerialization() {
        return DungeonLayout.class;
    }

}
