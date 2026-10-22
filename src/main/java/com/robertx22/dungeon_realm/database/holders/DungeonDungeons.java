package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.database.dungeon.Dungeon;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.dungeon_realm.room_adders.*;
import com.robertx22.library_of_exile.database.mob_list.MobList;
import com.robertx22.library_of_exile.database.mob_list.MobListTags;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.library_of_exile.tags.ExileTagRequirement;

public class DungeonDungeons extends ExileKeyHolder<Dungeon> {

    public static DungeonDungeons INSTANCE = new DungeonDungeons(DungeonMain.REGISTER_INFO);

    public DungeonDungeons(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<Dungeon, KeyInfo> CEMENTERY = ExileKey.ofId(this, "cement", x -> {
        return Dungeon.Builder.of(x.GUID(), "Cemetery", new CementeryAdder(), DungeonMain.MODID).weight(2000).build();
    });
    public ExileKey<Dungeon, KeyInfo> WARPED_FOREST = ExileKey.ofId(this, "warped", x -> {
        return Dungeon.Builder.of(x.GUID(), "Warped Forest", new WarpedRoomAdder(), DungeonMain.MODID)
                .tags(new ExileTagRequirement<MobList>().createBuilder().includes(MobListTags.FOREST).build()).weight(2000).build();

    });
    public ExileKey<Dungeon, KeyInfo> WIDE_NATURE = ExileKey.ofId(this, "wn", x -> {
        return Dungeon.Builder.of(x.GUID(), "Nature's End", new WideNatureRoomAdder(), DungeonMain.MODID).tags(new ExileTagRequirement<MobList>().createBuilder().includes(MobListTags.FOREST).build()).weight(2000).build();

    });
    public ExileKey<Dungeon, KeyInfo> BASTION = ExileKey.ofId(this, "bastion", x -> {
        return Dungeon.Builder.of(x.GUID(), "The Bastion", new BastionRoomAdder(), DungeonMain.MODID).weight(2000).build();
    });

    public ExileKey<Dungeon, KeyInfo> SEWER = ExileKey.ofId(this, "sewer2", x -> {
        return Dungeon.Builder.of(x.GUID(), "Slime Sewers", new Sewer2RoomAdder(), DungeonMain.MODID).weight(2000).build();
    });

    public ExileKey<Dungeon, KeyInfo> NATURE = ExileKey.ofId(this, "nature", x -> {
        return Dungeon.Builder.of(x.GUID(), "Natural", new NatureRoomAdder(), DungeonMain.MODID)
                .tags(new ExileTagRequirement<MobList>().createBuilder().includes(MobListTags.FOREST).build()).weight(300).build();
    });
    public ExileKey<Dungeon, KeyInfo> ICE_TEMPLE = ExileKey.ofId(this, "it", x -> {
        return Dungeon.Builder.of(x.GUID(), "Ice Temple", new IceTempleRoomAdder(), DungeonMain.MODID).weight(1000).build();
    });
    public ExileKey<Dungeon, KeyInfo> MOSSY_BRICK = ExileKey.ofId(this, "mossy_brick", x -> {
        return Dungeon.Builder.of(x.GUID(), "Mossy Temple", new MossyBrickRoomAdder(), DungeonMain.MODID).weight(750).build();
    });
    public ExileKey<Dungeon, KeyInfo> NETHER = ExileKey.ofId(this, "nether", x -> {
        return Dungeon.Builder.of(x.GUID(), "The Nether", new NetherRoomAdder(), DungeonMain.MODID).weight(750).build();
    });
    public ExileKey<Dungeon, KeyInfo> MINE = ExileKey.ofId(this, "mine", x -> {
        return Dungeon.Builder.of(x.GUID(), "The Mineshaft", new MineRoomAdder(), DungeonMain.MODID).weight(600).build();
    });
    public ExileKey<Dungeon, KeyInfo> SANDSTONE = ExileKey.ofId(this, "sandstone", x -> {
        return Dungeon.Builder.of(x.GUID(), "Sandstone", new SandstoneRoomAdder(), DungeonMain.MODID).weight(800).build();
    });
    public ExileKey<Dungeon, KeyInfo> SEWERS1 = ExileKey.ofId(this, "sewers", x -> {
        return Dungeon.Builder.of(x.GUID(), "Sewers", new SewersRoomAdder(), DungeonMain.MODID).weight(200).build();
    });
    public ExileKey<Dungeon, KeyInfo> SPRUCE_MANSION = ExileKey.ofId(this, "spruce_mansion", x -> {
        return Dungeon.Builder.of(x.GUID(), "Spruce Mansion", new SpruceMansionRoomAdder(), DungeonMain.MODID).weight(700).build();
    });
    public ExileKey<Dungeon, KeyInfo> STONE_BRICK = ExileKey.ofId(this, "stone_brick", x -> {
        return Dungeon.Builder.of(x.GUID(), "Stone Brick", new StoneBrickRoomAdder(), DungeonMain.MODID).weight(1000).build();
    });
    public ExileKey<Dungeon, KeyInfo> GIANT_TENTS = ExileKey.ofId(this, "tent", x -> {
        return Dungeon.Builder.of(x.GUID(), "Giant Tents", new TentRoomAdder(), DungeonMain.MODID).weight(600).build();
    });
    public ExileKey<Dungeon, KeyInfo> STEAMPUNK = ExileKey.ofId(this, "steampunk", x -> {
        return Dungeon.Builder.of(x.GUID(), "Steampunk", new SteampunkRoomAdder(), DungeonMain.MODID).weight(50).build();
    });
    public ExileKey<Dungeon, KeyInfo> NIGHT_TERROR = ExileKey.ofId(this, "night_terror", x -> {
        return Dungeon.Builder.of(x.GUID(), "Night Terror", new NightTerrorAdder(), DungeonMain.MODID).weight(1000).author("CTE2 Team").build();
    });

    /* this uh, LAGS?
    public ExileKey<Dungeon, KeyInfo> THE_END = ExileKey.ofId(this, "end", x -> {
        return Dungeon.Builder.of(x.GUID(), "The End", new EndAdder(), DungeonMain.MODID).weight(1000).author("DoutingDonut").build();
    });
     */

    @Override
    public void loadClass() {

    }
}
