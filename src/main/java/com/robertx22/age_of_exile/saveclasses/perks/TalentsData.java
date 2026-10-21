package com.robertx22.age_of_exile.saveclasses.perks;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.perks.PerkStatus;
import com.robertx22.age_of_exile.database.data.talent_tree.TalentTree;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.ExactStatData;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.IApplyableStats;
import com.robertx22.age_of_exile.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.age_of_exile.saveclasses.unit.stat_ctx.TalentStatCtx;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

import net.minecraft.network.Connection;
@Storable
public class TalentsData implements IApplyableStats {

    @Store
    private HashMap<String, SchoolData> perks = new HashMap<>();

    @Store
    public int reset_points = 5;

    public HashMap<String, SchoolData> getPerks(TalentTree.SchoolType type) {
        return perks;
    }

    public int getAllocatedPoints(TalentTree.SchoolType type) {
        int points = 0;
        for (Map.Entry<String, SchoolData> x : getPerks(type).entrySet()) {
            if (x.getValue() == null) {
                continue;
            }
            TalentTree school = ExileDB.TalentTrees().isRegistered(x.getKey())
                ? ExileDB.TalentTrees().get(x.getKey())
                : null;
            if (school != null && school.getSchool_type() == type) {
                points += x.getValue()
                    .getAllocatedPointsInSchool(school);
            }
        }
        return points;
    }

    public int getFreePoints(EntityData data, TalentTree.SchoolType type) {

        int num = 0;

        num = (int) GameBalanceConfig.get().STARTING_TALENT_POINTS;
        num += GameBalanceConfig.get().TALENT_POINTS_AT_MAX_LEVEL * LevelUtils.getMaxLevelMultiplier(data.getLevel());

        num -= this.getAllocatedPoints(type);
        return num;
    }

    public SchoolData getSchool(TalentTree school) {
        if (!getPerks(school.getSchool_type()).containsKey(school.GUID())) {
            getPerks(school.getSchool_type()).put(school.GUID(), new SchoolData());
        }
        return getPerks(school.getSchool_type()).get(school.GUID());
    }

    public void allocate(Player player, TalentTree school, PointData point) {
        getSchool(school).map.put(point, true);
    }

    public void remove(TalentTree school, PointData point) {
        getSchool(school).map.put(point, false);
    }

    public boolean hasFreePoints(EntityData data, TalentTree.SchoolType type) {
        return getFreePoints(data, type) > 0;
    }

    public boolean canAllocate(TalentTree school, PointData point, EntityData data, Player player) {
        if (school == null || point == null || data == null || player == null) {
            return false;
        }

        if (isAllocated(school, point)) {
            return false;
        }

        if (!hasFreePoints(data, school.getSchool_type())) {
            return false;
        }

        Perk perk = school.calcData.getPerk(point);
        if (perk == null) {
            return false;
        }

        if (perk.lvl_req > data.getLevel()) {
            return false;
        }

        if (perk.isLockedToPlayer(player)) {
            return false;
        }

        if (!perk.is_entry) {
            Set<PointData> con = school.calcData.connections.get(point);
            if (con == null || con.isEmpty()) {
                return false;
            }
            SchoolData schoolData = getSchool(school);
            boolean connected = false;
            for (PointData x : con) {
                if (schoolData.isAllocated(x)) {
                    connected = true;
                    break;
                }
            }
            if (!connected) {
                return false;
            }
        }

        return true;

    }

    public PointData findAllocatableEntryConnection(TalentTree school, PointData point, EntityData data, Player player) {
        if (school == null || point == null || data == null || player == null) {
            return null;
        }
        SchoolData schoolData = getSchool(school);
        if (!schoolData.getAllocatedPoints(school).isEmpty()) {
            return null;
        }
        Set<PointData> con = school.calcData.connections.get(point);
        if (con == null || con.isEmpty()) {
            return null;
        }
        for (PointData neighbor : con) {
            Perk neighborPerk = school.calcData.getPerk(neighbor);
            if (neighborPerk != null && neighborPerk.is_entry && canAllocate(school, neighbor, data, player)) {
                return neighbor;
            }
        }
        return null;
    }

    public String getFailAllocateReason(TalentTree school, PointData point, EntityData data, Player player) {
        if (school == null || point == null || data == null || player == null) {
            return "";
        }
        if (isAllocated(school, point)) {
            return "Талант уже изучен (ПКМ — сбросить)";
        }
        if (!hasFreePoints(data, school.getSchool_type())) {
            return "Недостаточно очков талантов";
        }
        Perk perk = school.calcData.getPerk(point);
        if (perk == null) {
            return "Неизвестный узел древа";
        }
        if (perk.lvl_req > data.getLevel()) {
            return "Требуется уровень: " + perk.lvl_req;
        }
        if (perk.isLockedToPlayer(player)) {
            if ("start".equals(perk.one_of_a_kind)) {
                return "Начальный путь уже выбран";
            }
            return "Уже выбран другой талант этого типа";
        }
        if (!perk.is_entry) {
            if (getSchool(school).getAllocatedPoints(school).isEmpty()) {
                return "Сначала выберите начальный путь в центре древа";
            }
            return "Сначала изучите соседний связанный талант";
        }
        return "";
    }

    public boolean canRemove(TalentTree school, PointData point) {

        if (!getSchool(school).isAllocated(point)) {
            return false;
        }

        if (reset_points < 1) {
            return false;
        }

        for (PointData con : school.calcData.connections.get(point)) {
            if (getSchool(school).isAllocated(con)) {
                Perk perk = school.calcData.getPerk(con);
                if (perk.is_entry) {
                    continue;
                }
                if (!hasPathToStart(school, con, point)) {
                    return false;
                }
            }
        }

        return true;

    }

    private boolean hasPathToStart(TalentTree school, PointData check, PointData toRemove) {
        Queue<PointData> openSet = new ArrayDeque<>();

        openSet.addAll(school.calcData.connections.get(check));

        Set<PointData> closedSet = new HashSet<>();

        while (!openSet.isEmpty()) {
            PointData current = openSet.poll();

            Perk perk = school.calcData.getPerk(current);

            if (current.equals(toRemove) || !getSchool(school).isAllocated(current)) {
                continue; // skip exploring this path
            }

            if (perk.is_entry) {
                return true;
            }

            if (!closedSet.add(current)) {
                continue; // we already visited it
            }

            openSet.addAll(school.calcData.connections.get(current));

        }

        return false;
    }

    public void clearAllTalents() {
        getPerks(TalentTree.SchoolType.TALENTS)
            .clear();

    }

    public HashMap<PointData, Perk> getAllAllocatedPerks() {
        HashMap<PointData, Perk> perks = new HashMap<>();
        for (TalentTree.SchoolType type : TalentTree.SchoolType.values()) {
            for (Map.Entry<String, SchoolData> x : getPerks(type)
                .entrySet()) {
                if (ExileDB.TalentTrees()
                    .isRegistered(x.getKey())) {

                    TalentTree school = ExileDB.TalentTrees()
                        .get(x.getKey());
                    if (school != null) {
                        for (PointData p : x.getValue()
                            .getAllocatedPoints(school)) {
                            perks.put(p, school.calcData.getPerk(p));
                        }
                    }
                }
            }
        }
        return perks;
    }

    public PerkStatus getStatus(Player player, TalentTree school, PointData point) {

        if (isAllocated(school, point)) {
            return PerkStatus.CONNECTED;
        }
        Perk perk = school.calcData.getPerk(point);
        if (perk == null) {
            return PerkStatus.BLOCKED;
        }

        if (perk.isLockedToPlayer(player)) {
            return PerkStatus.LOCKED_UNDER_ACHIEV;
        }

        if (canAllocate(school, point, Load.Unit(player), player)) {
            return PerkStatus.POSSIBLE;
        } else {
            return PerkStatus.BLOCKED;
        }
    }

    public Perk.Connection getConnection(TalentTree school, PointData one, PointData two) {

        if (isAllocated(school, one)) {

            if (isAllocated(school, two)) {
                return Perk.Connection.LINKED;
            }
            return Perk.Connection.POSSIBLE;
        } else if (isAllocated(school, two)) {
            return Perk.Connection.POSSIBLE;
        }

        return Perk.Connection.BLOCKED;
    }

    public boolean isAllocated(TalentTree school, PointData point) {
        return getSchool(school)
            .isAllocated(point);
    }

    @Override
    public List<StatContext> getStatAndContext(LivingEntity en) {
        List<StatContext> ctx = new ArrayList<>();

        HashMap<PointData, Perk> map = getAllAllocatedPerks();

        int lvl = Load.Unit(en)
            .getLevel();

        map.forEach((key, value) -> {
            List<ExactStatData> stats = new ArrayList<>();
            value.stats.forEach(s -> stats.add(s.toExactStat(lvl)));
            ctx.add(new TalentStatCtx(key, value, stats));

        });

        return ctx;
    }

}
