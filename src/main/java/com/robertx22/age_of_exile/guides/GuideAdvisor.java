package com.robertx22.age_of_exile.guides;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.database.data.game_balance_config.PlayerPointsType;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.spell_school.SpellSchool;
import com.robertx22.mine_and_slash.database.data.talent_tree.TalentTree;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.PointData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public final class GuideAdvisor {

    private static final List<GuideDefinition> BUILTIN = new ArrayList<>();

    static {
        BUILTIN.add(new GuideDefinition(
            "berserker_melee",
            "Воин Крови и Стали",
            "Ближний бой / Броня / Здоровье",
            ChatFormatting.RED,
            "sword",
            "warrior",
            0
        ).stat("strength", 0.7f).stat("vitality", 0.3f)
         .tip("Мощный фронтовой воин с высокой бронёй, регенерацией и физическим уроном."));

        BUILTIN.add(new GuideDefinition(
            "elemental_archmage",
            "Архимаг Стихий",
            "Заклинания / Мана / Энергощит",
            ChatFormatting.AQUA,
            "staff",
            "mage",
            2
        ).stat("intelligence", 0.75f).stat("wisdom", 0.25f)
         .tip("Дальнобойный заклинатель с высоким стихийным уроном и контролем толпы."));

        BUILTIN.add(new GuideDefinition(
            "phantom_marksman",
            "Призрачный Стрелок",
            "Луки / Уклонение / Критический удар",
            ChatFormatting.GREEN,
            "bow",
            "ranger",
            1
        ).stat("dexterity", 0.75f).stat("strength", 0.25f)
         .tip("Мобильный стрелок с высоким шансом крита, скоростью атаки и уклонением."));
    }

    private GuideAdvisor() {}

    public static List<GuideDefinition> getAllGuides() {
        return Collections.unmodifiableList(BUILTIN);
    }

    public static GuideDefinition getById(String id) {
        if (id == null || id.isBlank()) return BUILTIN.get(0);
        for (GuideDefinition g : BUILTIN) {
            if (g.id.equals(id)) return g;
        }
        return BUILTIN.get(0);
    }

    public static String getActiveGuideId(Player player) {
        if (player == null) return "";
        return Load.player(player).active_guide;
    }

    public static void setActiveGuide(ServerPlayer player, String id) {
        PlayerData data = Load.player(player);
        data.active_guide = id == null ? "" : id;
        data.syncToClient(player);
    }

    public static String autoApplyRecommended(ServerPlayer player) {
        PlayerData data = Load.player(player);
        EntityData unit = Load.Unit(player);
        GuideDefinition guide = getById(data.active_guide);

        int appliedStats = 0;
        String primaryStat = guide.stats.isEmpty() ? "strength" : guide.stats.get(0).stat;
        while (PlayerPointsType.STATS.getFreePoints(player) > 0 && ExileDB.Stats().isRegistered(primaryStat)) {
            data.statPoints.map.put(primaryStat, data.statPoints.map.getOrDefault(primaryStat, 0) + 1);
            appliedStats++;
        }

        int appliedTalents = 0;
        TalentTree talents = ExileDB.TalentTrees().getList().stream()
            .filter(t -> t.getSchool_type() == TalentTree.SchoolType.TALENTS)
            .findFirst().orElse(null);
        if (talents != null) {
            while (PlayerPointsType.TALENTS.getFreePoints(player) > 0) {
                PointData nextPoint = null;
                for (Map.Entry<PointData, String> e : talents.calcData.perks.entrySet()) {
                    if (data.talents.canAllocate(talents, e.getKey(), unit, player)) {
                        nextPoint = e.getKey();
                        break;
                    }
                }
                if (nextPoint == null) break;
                data.talents.allocate(player, talents, nextPoint);
                appliedTalents++;
            }
        }

        int appliedSpells = 0;
        SpellSchool school = ExileDB.SpellSchools().getList().stream().findFirst().orElse(null);
        if (school != null) {
            for (Map.Entry<String, PointData> e : school.perks.entrySet()) {
                if (PlayerPointsType.SPELLS.getFreePoints(player) <= 0) break;
                Perk perk = ExileDB.Perks().get(e.getKey());
                if (perk != null && school.isLevelEnoughFor(player, perk)) {
                    int cur = data.ascClass.allocated_lvls.getOrDefault(perk.GUID(), 0);
                    if (cur < perk.getMaxLevel()) {
                        data.ascClass.allocated_lvls.put(perk.GUID(), cur + 1);
                        appliedSpells++;
                    }
                }
            }
        }

        unit.forceRecalculateStats();
        data.syncToClient(player);
        unit.syncToClient(player);
        return "Вложено: статы +" + appliedStats + ", таланты +" + appliedTalents + ", заклинания +" + appliedSpells;
    }
}
