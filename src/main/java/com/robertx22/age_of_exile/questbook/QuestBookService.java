package com.robertx22.age_of_exile.questbook;

import com.robertx22.ancient_obelisks.database.ObeliskDatabase;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.database.data.profession.Profession;
import com.robertx22.mine_and_slash.database.data.runes.Rune;
import com.robertx22.mine_and_slash.database.data.talent_tree.TalentTree;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class QuestBookService {

    private QuestBookService() {}

    public static QuestBookData get(Player player) {
        PlayerData data = Load.player(player);
        if (data.questBook == null) {
            data.questBook = new QuestBookData();
        }
        return data.questBook;
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player.tickCount % 40 != 0) {
            return;
        }
        boolean changed = scanInventory(player);
        changed |= evaluateMilestones(player);
        if (changed) {
            Load.player(player).syncToClient(player);
        }
    }

    public static boolean scanInventory(Player player) {
        QuestBookData book = get(player);
        boolean changed = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (StackSaving.GEARS.has(stack)) {
                GearItemData gear = StackSaving.GEARS.loadFrom(stack);
                if (gear != null) {
                    if (gear.isUnique()) {
                        ExileStack ex = ExileStack.of(stack);
                        String uid = ex.get(StackKeys.CUSTOM).getOrCreate().data.get(com.robertx22.mine_and_slash.itemstack.CustomItemData.KEYS.UNIQUE_ID);
                        if (uid != null && !uid.isEmpty() && ExileDB.UniqueGears().isRegistered(uid)) {
                            changed |= book.uniqueGears.add(uid);
                        }
                    }
                    if (gear.sockets != null && gear.sockets.hasRuneWord()) {
                        changed |= book.runewords.add(gear.sockets.getRuneWord().GUID());
                    }
                }
            }
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (path.startsWith("rune/")) {
                String runeId = path.substring("rune/".length());
                if (ExileDB.Runes().isRegistered(runeId)) {
                    changed |= book.runes.add(runeId);
                }
            }
        }
        return changed;
    }

    public static void onProfessionUsed(Player player, String professionId) {
        if (player == null || professionId == null || professionId.isBlank()) return;
        QuestBookData book = get(player);
        if (book.professionsUsed.add(professionId) && player instanceof ServerPlayer sp) {
            Load.player(sp).syncToClient(sp);
        }
    }

    public static void onDungeonCompleted(Player player, String dungeonId) {
        if (player == null || dungeonId == null || dungeonId.isBlank()) return;
        QuestBookData book = get(player);
        if (book.infiniteDungeonLayouts.add(dungeonId) && player instanceof ServerPlayer sp) {
            Load.player(sp).syncToClient(sp);
        }
    }

    public static void onTowerBossKilled(LivingEntity boss, LivingEntity killer) {
        if (killer instanceof ServerPlayer sp && boss != null) {
            QuestBookData book = get(sp);
            String key = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString();
            book.towerBosses.add(key);
            book.towerThemes.add(key);
            evaluateMilestones(sp);
            Load.player(sp).syncToClient(sp);
        }
    }

    public static void onTowerCleared(Player player, String themeId) {
        if (player == null || themeId == null || themeId.isBlank()) return;
        QuestBookData book = get(player);
        if (book.towerThemes.add(themeId) && player instanceof ServerPlayer sp) {
            Load.player(sp).syncToClient(sp);
        }
    }

    public static void onUberBossKilled(Player player, String bossId) {
        if (player == null || bossId == null || bossId.isBlank()) return;
        QuestBookData book = get(player);
        if (book.missionGods.add(bossId) && player instanceof ServerPlayer sp) {
            Load.player(sp).syncToClient(sp);
        }
    }

    public static boolean evaluateMilestones(ServerPlayer player) {
        QuestBookData book = get(player);
        boolean changed = false;
        for (QuestBookCatalog.QuestEntry entry : QuestBookCatalog.ALL) {
            if (book.completedQuests.contains(entry.id())) continue;
            if (getProgress(player, entry) >= getTarget(entry)) {
                book.completedQuests.add(entry.id());
                changed = true;
                player.sendSystemMessage(Component.literal("★ Квест выполнен: " + entry.title())
                    .withStyle(ChatFormatting.GOLD));
            }
        }
        return changed;
    }

    public static int getTarget(QuestBookCatalog.QuestEntry entry) {
        return switch (entry.metric()) {
            case PROFESSIONS_USED, MAX_PROFESSIONS -> Math.max(1, ExileDB.Professions().getList().size());
            case RUNES -> Math.max(1, ExileDB.Runes().getList().size());
            case RUNEWORDS -> Math.max(1, ExileDB.RuneWords().getList().size());
            case UNIQUE_GEARS -> Math.max(1, ExileDB.UniqueGears().getList().size());
            case DUNGEON_LAYOUTS -> Math.max(1, DungeonDatabase.Dungeons().getList().size());
            case UBER_BOSSES -> Math.max(1, DungeonDatabase.UberBoss().getList().size() + ObeliskDatabase.getObelisks().getList().size());
            default -> Math.max(1, entry.target());
        };
    }

    public static int getProgress(Player player, QuestBookCatalog.QuestEntry entry) {
        if (player == null) return 0;
        QuestBookData book = get(player);
        PlayerData rpg = Load.player(player);
        return switch (entry.metric()) {
            case LEVEL -> Load.Unit(player).getLevel();
            case TALENT_POINTS -> rpg.talents.getAllocatedPoints(TalentTree.SchoolType.TALENTS)
                + rpg.talents.getAllocatedPoints(TalentTree.SchoolType.ASCENDANCY);
            case SPELLS_LEARNED -> rpg.ascClass.allocated_lvls.size();
            case STAT_POINTS -> rpg.statPoints.getAllocatedPoints();
            case PROFESSIONS_USED -> book.professionsUsed.size();
            case MAX_PROFESSIONS -> {
                int maxed = 0;
                for (Profession p : ExileDB.Professions().getList()) {
                    if (rpg.professions.getLevel(p.GUID()) >= 100) maxed++;
                }
                yield maxed;
            }
            case RUNES -> {
                int c = 0;
                for (Rune r : ExileDB.Runes().getList()) {
                    if (book.runes.contains(r.GUID())) c++;
                }
                yield c;
            }
            case RUNEWORDS -> (int) book.runewords.stream().filter(ExileDB.RuneWords()::isRegistered).count();
            case UNIQUE_GEARS -> (int) book.uniqueGears.stream().filter(ExileDB.UniqueGears()::isRegistered).count();
            case DUNGEON_LAYOUTS -> (int) book.infiniteDungeonLayouts.stream().filter(DungeonDatabase.Dungeons()::isRegistered).count();
            case TOWERS -> book.towerThemes.size();
            case DELVES -> book.delvesCompleted;
            case UBER_BOSSES -> book.missionGods.size();
            default -> book.completedQuests.contains(entry.id()) ? entry.target() : 0;
        };
    }

    public static void handleClientPacket(ServerPlayer player, QuestBookPacket packet) {
        scanInventory(player);
        evaluateMilestones(player);
        Load.player(player).syncToClient(player);
    }
}
