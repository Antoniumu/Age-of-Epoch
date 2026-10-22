package com.robertx22.library_of_exile.database.mob_list;

import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class LibMobLists extends ExileKeyHolder<MobList> {

    public static LibMobLists INSTANCE = new LibMobLists(Ref.REGISTER_INFO);


    public LibMobLists(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<MobList, KeyInfo> GENERIC_UNDEAD = ExileKey.ofId(this, "generic_undead", x -> {
        List<MobEntry> all = new ArrayList<>();
        all.add(new MobEntry(1000, net.minecraft.world.entity.EntityTypes.ZOMBIE));
        all.add(new MobEntry(300, net.minecraft.world.entity.EntityTypes.SKELETON));
        all.add(new MobEntry(500, net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("husk"))));
        return new MobList(x.GUID(), 1000, all);
    });
    public ExileKey<MobList, KeyInfo> SPIDER_FOREST = ExileKey.ofId(this, "spider_forest", x -> {
        List<MobEntry> all = new ArrayList<>();
        all.add(new MobEntry(1000, net.minecraft.world.entity.EntityTypes.SPIDER));
        all.add(new MobEntry(300, net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cave_spider"))));
        all.add(new MobEntry(100, net.minecraft.world.entity.EntityTypes.WITCH));
        all.add(new MobEntry(50, net.minecraft.world.entity.EntityTypes.STRAY));
        return new MobList(x.GUID(), 1000, all);
    });
    public ExileKey<MobList, KeyInfo> NETHER = ExileKey.ofId(this, "nether", x -> {
        List<MobEntry> all = new ArrayList<>();
        all.add(new MobEntry(1000, net.minecraft.world.entity.EntityTypes.WITHER_SKELETON));
        all.add(new MobEntry(300, net.minecraft.world.entity.EntityTypes.BLAZE));
        all.add(new MobEntry(500, net.minecraft.world.entity.EntityTypes.ZOMBIFIED_PIGLIN));
        return new MobList(x.GUID(), 1000, all, MobListTags.HAS_FLYING_MOBS);
    });
    public ExileKey<MobList, KeyInfo> EVIL_VILLAGER = ExileKey.ofId(this, "evil_villager", x -> {
        List<MobEntry> all = new ArrayList<>();
        all.add(new MobEntry(700, net.minecraft.world.entity.EntityTypes.ZOMBIE_VILLAGER));
        all.add(new MobEntry(300, net.minecraft.world.entity.EntityTypes.PILLAGER));
        all.add(new MobEntry(50, net.minecraft.world.entity.EntityTypes.EVOKER));
        all.add(new MobEntry(5, net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("vindicator"))));
        return new MobList(x.GUID(), 1000, all);
    });
    public ExileKey<MobList, KeyInfo> GREEN = ExileKey.ofId(this, "green", x -> {
        List<MobEntry> all = new ArrayList<>();
        all.add(new MobEntry(50, net.minecraft.world.entity.EntityTypes.SLIME));
        all.add(new MobEntry(300, net.minecraft.world.entity.EntityTypes.CREEPER));
        all.add(new MobEntry(50, net.minecraft.world.entity.EntityTypes.PHANTOM));
        all.add(new MobEntry(1000, net.minecraft.world.entity.EntityTypes.ZOMBIE));
        return new MobList(x.GUID(), 1000, all, MobListTags.HAS_FLYING_MOBS);
    });

    public ExileKey<MobList, KeyInfo> FOREST = ExileKey.ofId(this, "forest", x -> {
        List<MobEntry> all = new ArrayList<>();
        all.add(new MobEntry(700, net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cave_spider"))));
        all.add(new MobEntry(300, net.minecraft.world.entity.EntityTypes.SPIDER));
        all.add(new MobEntry(100, net.minecraft.world.entity.EntityTypes.WITCH));
        all.add(new MobEntry(33, net.minecraft.world.entity.EntityTypes.SLIME));
        return new MobList(x.GUID(), 1000, all, MobListTags.FOREST, MobListTags.HARVEST);
    });

    public ExileKey<MobList, KeyInfo> MAP_DEFAULT = ExileKey.ofId(this, "map_default", x -> {
        List<MobEntry> all = new ArrayList<>();
        all.add(new MobEntry(1000, net.minecraft.world.entity.EntityTypes.ZOMBIE));
        all.add(new MobEntry(250, net.minecraft.world.entity.EntityTypes.SKELETON));
        all.add(new MobEntry(25, net.minecraft.world.entity.EntityTypes.WITCH));
        all.add(new MobEntry(25, net.minecraft.world.entity.EntityTypes.SLIME));
        all.add(new MobEntry(100, net.minecraft.world.entity.EntityTypes.SPIDER));
        all.add(new MobEntry(100, net.minecraft.world.entity.EntityTypes.PILLAGER));
        return new MobList(x.GUID(), 1000, all, MobListTags.MAP);
    });

    @Override
    public void loadClass() {

    }
}
