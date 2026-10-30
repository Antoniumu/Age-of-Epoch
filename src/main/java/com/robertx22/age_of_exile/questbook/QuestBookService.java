package com.robertx22.age_of_exile.questbook;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.database.data.runewords.RuneWord;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.guides.GuideData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.saveclasses.spells.SpellsData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.RuneItem;
import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.world_of_exile.tower.TowerRoster;
import com.robertx22.world_of_exile.tower.TowerThemes;
import com.robertx22.world_of_exile.world_gen.TowerBoss;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative quest progress, persisted inside the player's RPG capability. */
public final class QuestBookService {

    private static final Set<UUID> DIRTY_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<UUID, Long> LAST_SYNC_NANOS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> LAST_REFRESH_NANOS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> LAST_TOGGLE_NANOS = new ConcurrentHashMap<>();

    private QuestBookService() {
    }

    public record Progress(int current, int target, boolean complete) {
    }

    private static QuestBookData data(Player player) {
        return Load.playerRPGData(player).questBook;
    }

    private static void maybeSync(ServerPlayer player, boolean force) {
        UUID id = player.getUUID();
        long now = System.nanoTime();
        long previous = LAST_SYNC_NANOS.getOrDefault(id, now - 1_000_000_000L);
        if (!force && (!DIRTY_PLAYERS.contains(id) || now - previous < 1_000_000_000L)) {
            return;
        }
        Load.playerRPGData(player).syncToClient(player);
        LAST_SYNC_NANOS.put(id, now);
        DIRTY_PLAYERS.remove(id);
    }

    private static boolean maybeAwardPlatinum(ServerPlayer player) {
        QuestBookData book = data(player);
        if (!book.platinumMode || book.platinumAwarded || !isPlatinumReady(player)) {
            return false;
        }

        book.platinumAwarded = true;
        player.sendSystemMessage(Component.literal("✦ ПЛАТИНА AGE OF EPOCH ✦")
            .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("Ты закрыл основную кампанию и весь зарегистрированный контент. Звание сохранено в квестбуке.")
            .withStyle(net.minecraft.ChatFormatting.YELLOW));
        return true;
    }

    private static void afterProgress(ServerPlayer player, boolean changed) {
        if (!changed) {
            return;
        }
        DIRTY_PLAYERS.add(player.getUUID());
        if (maybeAwardPlatinum(player)) {
            maybeSync(player, true);
        } else {
            maybeSync(player, false);
        }
    }

    public static void onLogin(ServerPlayer player) {
        scanInventory(player);
        maybeAwardPlatinum(player);
        maybeSync(player, true);
    }

    public static void onLogout(ServerPlayer player) {
        UUID id = player.getUUID();
        DIRTY_PLAYERS.remove(id);
        LAST_SYNC_NANOS.remove(id);
        LAST_REFRESH_NANOS.remove(id);
        LAST_TOGGLE_NANOS.remove(id);
    }

    /** Called once a second: inventory collection scan and a low-cost completion check. */
    public static void onPlayerSecond(ServerPlayer player) {
        boolean changed = scanInventory(player);
        boolean awarded = maybeAwardPlatinum(player);
        if (changed) {
            DIRTY_PLAYERS.add(player.getUUID());
        }
        if (awarded) {
            maybeSync(player, true);
        } else {
            maybeSync(player, false);
        }
    }

    private static boolean scanInventory(ServerPlayer player) {
        QuestBookData book = data(player);
        boolean changed = false;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            changed |= recordStack(book, player.getInventory().getItem(i));
        }
        changed |= recordStack(book, player.getItemBySlot(EquipmentSlot.HEAD));
        changed |= recordStack(book, player.getItemBySlot(EquipmentSlot.CHEST));
        changed |= recordStack(book, player.getItemBySlot(EquipmentSlot.LEGS));
        changed |= recordStack(book, player.getItemBySlot(EquipmentSlot.FEET));
        changed |= recordStack(book, player.getOffhandItem());

        afterProgress(player, changed);
        return changed;
    }

    private static boolean recordStack(QuestBookData book, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        boolean changed = false;
        if (stack.getItem() instanceof GemItem gem && gem.gemType != null && gem.gemRank != null) {
            changed |= book.gemVariants.add(gem.gemType.id + ":" + gem.gemRank.tier);
        }
        if (stack.getItem() instanceof RuneItem rune && rune.type != null) {
            changed |= book.runes.add(rune.type.id);
        }
        if (StackSaving.GEARS.has(stack)) {
            try {
                var gear = StackSaving.GEARS.loadFrom(stack);
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                boolean itemMatches = gear != null && (gear.item_id == null || gear.item_id.isBlank()
                    || gear.item_id.equals(itemId));
                if (itemMatches && gear.uniq_id != null && !gear.uniq_id.isBlank()
                    && ExileDB.UniqueGears().isRegistered(gear.uniq_id)) {
                    changed |= book.uniqueGears.add(gear.uniq_id);
                }
            } catch (Exception ignored) {
                // Keep scanning other stacks if an old or malformed item has stale NBT.
            }
        }
        return changed;
    }

    public static void onMobKilled(LivingEntity mob, LivingEntity killer) {
        if (!(killer instanceof ServerPlayer player) || mob == null || mob instanceof Player) {
            return;
        }
        QuestBookData book = data(player);
        book.mobKills++;
        boolean changed = true;

        if (mob.entityTags().contains(TowerBoss.TAG)) {
            String themeId = null;
            String bossId = null;
            for (String tag : mob.entityTags()) {
                if (tag.startsWith(TowerBoss.THEME_TAG_PREFIX)) {
                    themeId = tag.substring(TowerBoss.THEME_TAG_PREFIX.length());
                } else if (tag.startsWith(TowerBoss.DEFINITION_TAG_PREFIX)) {
                    bossId = tag.substring(TowerBoss.DEFINITION_TAG_PREFIX.length());
                }
            }
            if (themeId != null && bossId != null && isKnownTowerBoss(themeId, bossId)) {
                com.robertx22.age_of_exile.airship.adventure.AdventureService.mechanic(player,com.robertx22.age_of_exile.airship.adventure.Voyage.Metric.TOWER,themeId+":"+bossId,mob.getUUID().toString());
                changed |= book.towerBosses.add(themeId + ":" + bossId);
                changed |= book.towerThemes.add(themeId);
            }
        }

        afterProgress(player, changed);
    }

    /** Called before the tower event handler strips its boss tags on death. */
    public static void onTowerBossKilled(LivingEntity mob, LivingEntity killer) {
        if (!(killer instanceof ServerPlayer player) || mob == null || !mob.entityTags().contains(TowerBoss.TAG)) {
            return;
        }
        String themeId = null;
        String bossId = null;
        for (String tag : mob.entityTags()) {
            if (tag.startsWith(TowerBoss.THEME_TAG_PREFIX)) {
                themeId = tag.substring(TowerBoss.THEME_TAG_PREFIX.length());
            } else if (tag.startsWith(TowerBoss.DEFINITION_TAG_PREFIX)) {
                bossId = tag.substring(TowerBoss.DEFINITION_TAG_PREFIX.length());
            }
        }
        if (themeId == null || bossId == null || !isKnownTowerBoss(themeId, bossId)) {
            return;
        }
        QuestBookData book = data(player);
        com.robertx22.age_of_exile.airship.adventure.AdventureService.mechanic(player,com.robertx22.age_of_exile.airship.adventure.Voyage.Metric.TOWER,themeId+":"+bossId,mob.getUUID().toString());
        boolean changed = book.towerBosses.add(towerBossKey(themeId, bossId));
        changed |= book.towerThemes.add(themeId);
        afterProgress(player, changed);
    }

    public static void onChestLooted(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        data(serverPlayer).chestsLooted++;
        afterProgress(serverPlayer, true);
    }

    public static void onProfessionUsed(Player player, PlayerSkillEnum skill) {
        if (!(player instanceof ServerPlayer serverPlayer) || skill == null
            || skill == PlayerSkillEnum.ALL || skill == PlayerSkillEnum.NONE) {
            return;
        }
        com.robertx22.age_of_exile.airship.adventure.AdventureService.mechanic(serverPlayer,com.robertx22.age_of_exile.airship.adventure.Voyage.Metric.PROFESSION,skill.id,UUID.randomUUID().toString());
        afterProgress(serverPlayer, data(serverPlayer).professionsUsed.add(skill.id));
    }

    public static void onStandardDungeonCompleted(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        com.robertx22.age_of_exile.airship.adventure.AdventureService.mechanic(serverPlayer,com.robertx22.age_of_exile.airship.adventure.Voyage.Metric.DUNGEON,"standard",UUID.randomUUID().toString());
        data(serverPlayer).standardDungeonsCompleted++;
        afterProgress(serverPlayer, true);
    }

    public static void onInfiniteDungeonCompleted(Player player, String layoutId) {
        if (!(player instanceof ServerPlayer serverPlayer) || layoutId == null || layoutId.isBlank()) {
            return;
        }
        QuestBookData book = data(serverPlayer);
        book.infiniteDungeonsCompleted++;
        book.infiniteDungeonLayouts.add(layoutId);
        afterProgress(serverPlayer, true);
    }

    public static void onMissionCompleted(Player player, String godId) {
        if (!(player instanceof ServerPlayer serverPlayer) || godId == null || godId.isBlank()) {
            return;
        }
        com.robertx22.age_of_exile.airship.adventure.AdventureService.mechanic(serverPlayer,com.robertx22.age_of_exile.airship.adventure.Voyage.Metric.MISSION,godId,UUID.randomUUID().toString());
        afterProgress(serverPlayer, data(serverPlayer).missionGods.add(godId));
    }

    public static void onRunewordCreated(Player player, RuneWord word) {
        if (!(player instanceof ServerPlayer serverPlayer) || word == null || word.GUID() == null) {
            return;
        }
        com.robertx22.age_of_exile.airship.adventure.AdventureService.mechanic(serverPlayer,com.robertx22.age_of_exile.airship.adventure.Voyage.Metric.RUNEWORD,word.GUID(),UUID.randomUUID().toString());
        afterProgress(serverPlayer, data(serverPlayer).runewords.add(word.GUID()));
    }

    public static void onSimulationGuideApplied(ServerPlayer player, String guideId) {
        if (player == null || guideId == null || guideId.isBlank()) {
            return;
        }
        afterProgress(player, data(player).simulationGuides.add(guideId));
    }

    public static void refresh(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = System.nanoTime();
        Long previous = LAST_REFRESH_NANOS.get(id);
        if (previous != null && now - previous < 1_000_000_000L) {
            return;
        }
        LAST_REFRESH_NANOS.put(id, now);

        boolean changed = scanInventory(player);
        if (changed) {
            DIRTY_PLAYERS.add(player.getUUID());
        }
        maybeAwardPlatinum(player);
        maybeSync(player, true);
    }

    public static void togglePlatinum(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = System.nanoTime();
        Long previous = LAST_TOGGLE_NANOS.get(id);
        if (previous != null && now - previous < 500_000_000L) {
            return;
        }
        LAST_TOGGLE_NANOS.put(id, now);

        QuestBookData book = data(player);
        book.platinumMode = !book.platinumMode;
        player.sendSystemMessage(Component.literal(book.platinumMode
            ? "Платиновое испытание включено. Коллекции отмечаются в фоне; звание выдаётся после выполнения всех условий."
            : "Платиновое испытание выключено. Уже полученное звание Платины не отзывается.")
            .withStyle(book.platinumMode ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GRAY));
        maybeAwardPlatinum(player);
        maybeSync(player, true);
    }

    public static void claimManualTask(ServerPlayer player, String taskId) {
        QuestBookCatalog.Objective objective = QuestBookCatalog.findObjective(taskId);
        if (objective == null || objective.metric() != QuestBookCatalog.Metric.MANUAL) {
            return;
        }
        QuestBookData book = data(player);
        if (book.manualTasks.add(taskId)) {
            player.sendSystemMessage(Component.literal("Запись квестбука отмечена: " + objective.title())
                .withStyle(net.minecraft.ChatFormatting.GREEN));
            afterProgress(player, true);
            maybeAwardPlatinum(player);
            maybeSync(player, true);
        }
    }

    public static Progress progress(Player player, QuestBookCatalog.Objective objective) {
        int target = target(player, objective);
        int current = current(player, objective);
        boolean complete = target > 0 && current >= target;
        return new Progress(Math.min(current, target), target, complete);
    }

    public static int completedObjectives(Player player, QuestBookCatalog.Chapter chapter) {
        int completed = 0;
        for (QuestBookCatalog.Objective objective : chapter.objectives()) {
            if (progress(player, objective).complete()) {
                completed++;
            }
        }
        return completed;
    }

    public static boolean chapterComplete(Player player, QuestBookCatalog.Chapter chapter) {
        return completedObjectives(player, chapter) == chapter.objectives().size();
    }

    public static int completedStoryChapters(Player player) {
        int completed = 0;
        for (QuestBookCatalog.Chapter chapter : QuestBookCatalog.chapters()) {
            if (!chapter.platinum() && chapterComplete(player, chapter)) {
                completed++;
            }
        }
        return completed;
    }

    public static boolean isPlatinumReady(Player player) {
        QuestBookCatalog.Chapter platinum = QuestBookCatalog.findChapter("platinum");
        return platinum != null && chapterComplete(player, platinum);
    }

    public static int target(Player player, QuestBookCatalog.Objective objective) {
        if (objective.target() >= 0) {
            return objective.target();
        }
        return switch (objective.metric()) {
            case PLAYER_LEVEL -> maxCharacterLevel();
            case PROFESSIONS_USED, MAX_PROFESSIONS -> PlayerSkillEnum.getAll().size();
            case GEM_TYPES -> GemItem.GemType.values().length;
            case GEM_VARIANTS -> GemItem.GemType.values().length * GemItem.GemRank.values().length;
            case RUNES -> RuneItem.RuneType.values().length;
            case UNIQUE_GEARS -> Math.max(1, safeUniqueGearCount());
            case RUNEWORDS -> Math.max(1, safeRunewordCount());
            case INFINITE_DUNGEONS -> Math.max(1, safeDungeonLayoutCount());
            case MISSION_GODS -> Math.max(1, safeGodCount());
            case TOWER_BOSSES -> TowerRoster.size();
            case TOWER_THEMES -> TowerThemes.all().size();
            case SIMULATION_GUIDES -> GuideData.all().size();
            case STORY_CHAPTERS -> QuestBookCatalog.storyChapterCount();
            default -> 1;
        };
    }

    public static int current(Player player, QuestBookCatalog.Objective objective) {
        QuestBookData book = data(player);
        return switch (objective.metric()) {
            case MANUAL -> book.manualTasks.contains(objective.id()) ? 1 : 0;
            case PLAYER_LEVEL -> safePlayerLevel(player);
            case MOB_KILLS -> book.mobKills;
            case CHESTS -> book.chestsLooted;
            case STAT_POINTS -> statPointCount(player);
            case TALENTS -> talentCount(player);
            case SPELLS -> spellPointCount(player);
            case PROFESSIONS_USED -> book.professionsUsed.size();
            case GEM_TYPES -> countGemTypes(book);
            case GEM_VARIANTS -> countKnownGems(book);
            case RUNES -> countKnownRunes(book);
            case UNIQUE_GEARS -> countKnownUniqueGears(book);
            case RUNEWORDS -> countKnownRunewords(book);
            case STANDARD_DUNGEONS -> book.standardDungeonsCompleted;
            case INFINITE_DUNGEONS -> countKnownDungeons(book);
            case MISSION_GODS -> countKnownGods(book);
            case TOWER_BOSSES -> countKnownTowerBosses(book);
            case TOWER_THEMES -> countKnownTowerThemes(book);
            case MAX_PROFESSIONS -> maxedProfessionCount(player);
            case SIMULATION_GUIDES -> countKnownGuides(book);
            case STORY_CHAPTERS -> completedStoryChapters(player);
        };
    }

    public static int gemVariantCount() {
        return GemItem.GemType.values().length * GemItem.GemRank.values().length;
    }

    public static String gemKey(GemItem.GemType type, GemItem.GemRank rank) {
        return type.id + ":" + rank.tier;
    }

    public static String towerBossKey(String themeId, String bossId) {
        return themeId + ":" + bossId;
    }

    public static boolean isKnownTowerBoss(String themeId, String bossId) {
        if (!TowerThemes.exists(themeId) || bossId == null) {
            return false;
        }
        return TowerRoster.all(themeId).stream().anyMatch(def -> def.id().equals(bossId));
    }

    private static int maxCharacterLevel() {
        try {
            GameBalanceConfig config = GameBalanceConfig.get();
            return config == null || config.MAX_LEVEL < 1 ? 50 : config.MAX_LEVEL;
        } catch (Exception ignored) {
            return 50;
        }
    }

    private static int safePlayerLevel(Player player) {
        try {
            EntityData unit = Load.Unit(player);
            return unit == null ? 0 : unit.getLevel();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int statPointCount(Player player) {
        try {
            return Load.playerRPGData(player).statPoints.map.values().stream().mapToInt(Integer::intValue).sum();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int talentCount(Player player) {
        try {
            return Load.playerRPGData(player).talents.getAllAllocatedPerks().size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int spellPointCount(Player player) {
        try {
            SpellsData spells = Load.spells(player).getSpellsData();
            return spells.allocated_lvls.values().stream().mapToInt(Integer::intValue).sum();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int countGemTypes(QuestBookData book) {
        Set<String> ids = new HashSet<>();
        for (String key : book.gemVariants) {
            String[] parts = key.split(":", 2);
            if (parts.length == 2 && isKnownGemType(parts[0])) {
                ids.add(parts[0]);
            }
        }
        return ids.size();
    }

    private static int countKnownGems(QuestBookData book) {
        int count = 0;
        for (String key : book.gemVariants) {
            String[] parts = key.split(":", 2);
            if (parts.length != 2 || !isKnownGemType(parts[0])) {
                continue;
            }
            try {
                int tier = Integer.parseInt(parts[1]);
                if (tier >= 0 && tier < GemItem.GemRank.values().length) {
                    count++;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return count;
    }

    private static boolean isKnownGemType(String id) {
        for (GemItem.GemType type : GemItem.GemType.values()) {
            if (type.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static int countKnownRunes(QuestBookData book) {
        int count = 0;
        for (RuneItem.RuneType type : RuneItem.RuneType.values()) {
            if (book.runes.contains(type.id)) {
                count++;
            }
        }
        return count;
    }

    private static int countKnownUniqueGears(QuestBookData book) {
        try {
            return (int) book.uniqueGears.stream().filter(ExileDB.UniqueGears()::isRegistered).count();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int countKnownRunewords(QuestBookData book) {
        try {
            return (int) book.runewords.stream().filter(ExileDB.RuneWords()::isRegistered).count();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int countKnownDungeons(QuestBookData book) {
        try {
            return (int) book.infiniteDungeonLayouts.stream().filter(DungeonsDB.DungeonLayouts()::isRegistered).count();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int countKnownGods(QuestBookData book) {
        try {
            return (int) book.missionGods.stream().filter(MissionsDB.Gods()::isRegistered).count();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int countKnownTowerBosses(QuestBookData book) {
        int count = 0;
        for (String key : book.towerBosses) {
            String[] parts = key.split(":", 2);
            if (parts.length == 2 && isKnownTowerBoss(parts[0], parts[1])) {
                count++;
            }
        }
        return count;
    }

    private static int countKnownTowerThemes(QuestBookData book) {
        return (int) book.towerThemes.stream().filter(TowerThemes::exists).count();
    }

    private static int maxedProfessionCount(Player player) {
        try {
            RPGPlayerData rpg = Load.playerRPGData(player);
            int count = 0;
            for (PlayerSkillEnum skill : PlayerSkillEnum.getAll()) {
                if (rpg.professions.getProfessionLevel(skill) >= PlayerSkillData.MAX_LEVEL) {
                    count++;
                }
            }
            return count;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int countKnownGuides(QuestBookData book) {
        return (int) book.simulationGuides.stream().filter(id -> GuideData.byId(id) != null).count();
    }

    private static int safeUniqueGearCount() {
        try {
            return (int) ExileDB.UniqueGears().getList().stream()
                .map(UniqueGear::GUID)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .count();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int safeRunewordCount() {
        try {
            return (int) ExileDB.RuneWords().getList().stream()
                .map(RuneWord::GUID)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .count();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int safeDungeonLayoutCount() {
        try {
            return (int) DungeonsDB.DungeonLayouts().getList().stream()
                .map(layout -> layout.GUID())
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .count();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int safeGodCount() {
        try {
            return (int) MissionsDB.Gods().getList().stream()
                .map(god -> god.GUID())
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .count();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
