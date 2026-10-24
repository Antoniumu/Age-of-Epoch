package com.robertx22.age_of_exile.capability.player;

import com.robertx22.age_of_exile.capability.bases.ICommonPlayerCap;
import com.robertx22.age_of_exile.capability.player.data.*;
import com.robertx22.age_of_exile.database.data.tiers.base.Difficulty;
import com.robertx22.age_of_exile.dimension.dungeon_data.DungeonData;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.DeathStatsData;
import com.robertx22.age_of_exile.questbook.QuestBookData;
import com.robertx22.age_of_exile.saveclasses.perks.TalentsData;
import com.robertx22.age_of_exile.saveclasses.player_skills.ProfessionsData;
import com.robertx22.age_of_exile.tpa.TpaData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.components.forge.BaseProvider;
import com.robertx22.library_of_exile.components.forge.BaseStorage;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityInject;
import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;
import com.robertx22.forge_compat.eventbus.api.SubscribeEvent;
import com.robertx22.forge_compat.fml.common.Mod;

@Mod.EventBusSubscriber
public class RPGPlayerData implements ICommonPlayerCap {

    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(SlashRef.MODID, "player_data");

    @Mod.EventBusSubscriber
    public static class EventHandler {
        @SubscribeEvent
        public static void onEntityConstruct(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(RESOURCE, new Provider((Player) event.getObject()));
            }
        }
    }

    public static class Storage implements BaseStorage<RPGPlayerData> {

    }

    public static class Provider extends BaseProvider<RPGPlayerData, Player> {
        public Provider(Player owner) {
            super(owner);
        }

        @Override
        public RPGPlayerData newDefaultImpl(Player owner) {
            return new RPGPlayerData(owner);
        }

        @Override
        public Capability<RPGPlayerData> dataInstance() {
            return Data;
        }
    }

    @CapabilityInject(RPGPlayerData.class)
    public static Capability<RPGPlayerData> Data = null;
    // stupid bloatware

    private static final String MAP_DATA = "maps";
    private static final String TEAM_DATA = "teams";
    private static final String FAVOR_DATA = "favor";
    private static final String TALENTS_DATA = "talents";
    private static final String STAT_POINTS = "stat_points";
    private static final String DEATH_STATS = "death_stats";
    private static final String PROFESSIONS = "profs";
    private static final String DIFF = "diff";
    private static final String ACTIVE_GUIDE = "active_guide";
    private static final String QUEST_BOOK = "quest_book";
    private static final String TPA_DATA = "tpa";

    Player player;

    public MapsData maps = new MapsData();
    public TeamData team = new TeamData();
    public FavorData favor = new FavorData();
    public TalentsData talents = new TalentsData();
    public StatPointsData statPoints = new StatPointsData();
    public DeathStatsData deathStats = new DeathStatsData();
    public ProfessionsData professions = new ProfessionsData();
    public ScalingPlayerDiffData scalingDifficulty = new ScalingPlayerDiffData();
    public QuestBookData questBook = new QuestBookData();
    public TpaData tpa = new TpaData();

    /**
     * id выбранного гайда (см. guides/GuideData). Пустая строка = гайд не выбран.
     * Хранится в кэпе, поэтому автоматически синхронизируется на клиент.
     */
    public String active_guide = "";

    public RPGPlayerData(Player player) {
        this.player = player;
    }

    public void createRandomDungeon(Difficulty diff) {

        this.maps = new MapsData();
        maps.isEmpty = false;

        DungeonData dun = new DungeonData();
        int lvl = Load.Unit(player)
            .getLevel();
        dun.randomize(lvl, diff);

        this.maps.dungeonData = dun;

        this.syncToClient(player);
    }

    @Override
    public CompoundTag saveToNBT() {

        CompoundTag nbt = new CompoundTag();

        LoadSave.Save(maps, nbt, MAP_DATA);
        LoadSave.Save(team, nbt, TEAM_DATA);
        LoadSave.Save(favor, nbt, FAVOR_DATA);
        LoadSave.Save(talents, nbt, TALENTS_DATA);
        LoadSave.Save(statPoints, nbt, STAT_POINTS);
        LoadSave.Save(deathStats, nbt, DEATH_STATS);
        LoadSave.Save(professions, nbt, PROFESSIONS);
        LoadSave.Save(scalingDifficulty, nbt, DIFF);
        LoadSave.Save(questBook, nbt, QUEST_BOOK);
        nbt.put(TPA_DATA, tpa.saveToNBT());
        nbt.putString(ACTIVE_GUIDE, active_guide == null ? "" : active_guide);

        return nbt;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {

        this.maps = loadOrBlank(MapsData.class, new MapsData(), nbt, MAP_DATA, new MapsData());
        this.team = loadOrBlank(TeamData.class, new TeamData(), nbt, TEAM_DATA, new TeamData());
        this.favor = loadOrBlank(FavorData.class, new FavorData(), nbt, FAVOR_DATA, new FavorData());
        this.talents = loadOrBlank(TalentsData.class, new TalentsData(), nbt, TALENTS_DATA, new TalentsData());
        this.statPoints = loadOrBlank(StatPointsData.class, new StatPointsData(), nbt, STAT_POINTS, new StatPointsData());
        this.deathStats = loadOrBlank(DeathStatsData.class, new DeathStatsData(), nbt, DEATH_STATS, new DeathStatsData());
        this.professions = loadOrBlank(ProfessionsData.class, new ProfessionsData(), nbt, PROFESSIONS, new ProfessionsData());
        this.scalingDifficulty = loadOrBlank(ScalingPlayerDiffData.class, new ScalingPlayerDiffData(), nbt, DIFF, new ScalingPlayerDiffData());
        this.questBook = loadOrBlank(QuestBookData.class, new QuestBookData(), nbt, QUEST_BOOK, new QuestBookData());
        this.tpa = new TpaData();
        this.tpa.loadFromNBT(nbt.getCompoundOrEmpty(TPA_DATA));
        this.active_guide = nbt.getStringOr(ACTIVE_GUIDE, "");

    }

    public static <OBJ> OBJ loadOrBlank(Class theclass, OBJ newobj, CompoundTag nbt, String loc, OBJ blank) {
        try {
            OBJ data = LoadSave.Load(theclass, newobj, nbt, loc);
            if (data == null) {
                return blank;
            } else {
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blank;
    }

    @Override
    public String getCapIdForSyncing() {
        return "rpg_player_data";
    }

}
