package com.robertx22.age_of_exile.simulation;

import com.robertx22.age_of_exile.a_libraries.curios.MyCurioUtils;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.capability.player.data.StatPointsData;
import com.robertx22.age_of_exile.database.OptScaleExactStat;
import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.rarities.MobRarity;
import com.robertx22.age_of_exile.database.data.stats.StatScaling;
import com.robertx22.age_of_exile.database.data.stats.types.core_stats.AllAttributes;
import com.robertx22.age_of_exile.database.data.stats.types.defense.Armor;
import com.robertx22.age_of_exile.database.data.stats.types.defense.DodgeRating;
import com.robertx22.age_of_exile.database.data.stats.types.defense.SpellDodge;
import com.robertx22.age_of_exile.database.data.stats.types.generated.ElementalResist;
import com.robertx22.age_of_exile.database.data.stats.types.resources.health.Health;
import com.robertx22.age_of_exile.database.data.talent_tree.TalentTree;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.dimension.DimensionIds;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CurrencyItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.saveclasses.perks.TalentsData;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.saveclasses.unit.Unit;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.effectdatas.DamageEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.EventBuilder;
import com.robertx22.age_of_exile.uncommon.effectdatas.rework.EventData;
import com.robertx22.age_of_exile.uncommon.effectdatas.rework.RestoreType;
import com.robertx22.age_of_exile.uncommon.enumclasses.AttackType;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;
import com.robertx22.age_of_exile.uncommon.enumclasses.PlayStyle;
import com.robertx22.age_of_exile.uncommon.enumclasses.WeaponTypes;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.age_of_exile.uncommon.utilityclasses.HealthUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.NumberUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SimulationManager {

    private static final Map<UUID, CompoundTag> SERVER_SNAPSHOTS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ItemStack>> IN_MEMORY_INV_SNAPSHOTS = new ConcurrentHashMap<>();

    public static final BlockPos ARENA_CENTER = new BlockPos(0, 64, 0);
    public static final BlockPos DUMMY_POS = new BlockPos(0, 64, 8);
    public static final BlockPos EXIT_PORTAL_POS = new BlockPos(0, 64, -8);

    public static boolean isInSimulation(Player player) {
        if (player == null) {
            return false;
        }
        return WorldUtils.isSimulationWorld(player.level()) || hasActiveSnapshot(player.getUUID());
    }

    public static boolean hasActiveSnapshot(UUID uuid) {
        CompoundTag tag = SERVER_SNAPSHOTS.get(uuid);
        return tag != null && tag.getBooleanOr("active", false);
    }

    public static CompoundTag getSnapshotForSave(Entity entity) {
        if (entity instanceof Player) {
            CompoundTag snap = SERVER_SNAPSHOTS.get(entity.getUUID());
            if (snap != null && snap.getBooleanOr("active", false)) {
                return snap.copy();
            }
        }
        return null;
    }

    public static void loadSnapshotFromSave(Entity entity, CompoundTag snap) {
        if (entity instanceof Player && snap != null && snap.getBooleanOr("active", false)) {
            SERVER_SNAPSHOTS.put(entity.getUUID(), snap.copy());
        }
    }

    public static void toggleSimulation(ServerPlayer player) {
        if (WorldUtils.isSimulationWorld(player.level()) || hasActiveSnapshot(player.getUUID())) {
            exitSimulation(player);
        } else {
            enterSimulation(player);
        }
    }

    public static void enterSimulation(ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (!hasActiveSnapshot(player.getUUID()) && !WorldUtils.isSimulationWorld(player.level())) {
            savePlayerSnapshot(player);
        }

        ServerLevel simLevel = getSimulationLevel(player);
        if (simLevel != null) {
            buildArena(simLevel);
            TeleportUtils.teleport(player, ARENA_CENTER, DimensionIds.SIMULATION_DIMENSION);
        }

        RPGPlayerData rpgData = Load.playerRPGData(player);
        rpgData.talents.reset_points = Math.max(rpgData.talents.reset_points, 999);

        EntityData unitData = Load.Unit(player);
        unitData.setEquipsChanged(true);
        unitData.tryRecalculateStats();
        healPlayerFull(player);

        syncAll(player);

        player.sendSystemMessage(Component.literal("══════════════════════════════════════════").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("✦ ПОЛИГОН СИМУЛЯЦИИ БИЛДОВ (Age of Epoch) ✦").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("• Ваше исходное состояние и инвентарь сохранены в снимке.").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("• Все изменения уровня, талантов, характеристик и предметов действуют только на полигоне.").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("• Откройте меню Полигона через экран персонажа [H -> Полигон] или командой /sim.").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("• Для выхода и 100% возврата состояния войдите в портал позади вас или нажмите «Выйти» в меню.").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("══════════════════════════════════════════").withStyle(ChatFormatting.GOLD));
    }

    public static void exitSimulation(ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (WorldUtils.isSimulationWorld(player.level())) {
            clearSimulationMobs(player, false);
        }

        CompoundTag snap = SERVER_SNAPSHOTS.remove(player.getUUID());
        List<ItemStack> memInv = IN_MEMORY_INV_SNAPSHOTS.remove(player.getUUID());

        if (snap != null && snap.getBooleanOr("active", false)) {
            restoreFromSnapshot(player, snap, memInv);
            player.sendSystemMessage(Component.literal("§a[Полигон] Симуляция завершена. Ваш персонаж, уровень, таланты и инвентарь полностью восстановлены!"));
        } else if (WorldUtils.isSimulationWorld(player.level())) {
            BlockPos spawnPos = new BlockPos(0, 80, 0);
            TeleportUtils.teleport(player, spawnPos, Identifier.withDefaultNamespace("overworld"));
            player.sendSystemMessage(Component.literal("§e[Полигон] Вы покинули измерение симуляции."));
        }
    }

    public static void checkAndRestoreIfLeftSimulation(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (hasActiveSnapshot(player.getUUID()) && !WorldUtils.isSimulationWorld(player.level())) {
            CompoundTag snap = SERVER_SNAPSHOTS.remove(player.getUUID());
            List<ItemStack> memInv = IN_MEMORY_INV_SNAPSHOTS.remove(player.getUUID());
            if (snap != null) {
                restoreCapabilitiesAndInventory(player, snap, memInv);
                player.sendSystemMessage(Component.literal("§a[Полигон] Вы покинули измерение симуляции — исходное состояние персонажа и инвентарь восстановлены!"));
            }
        }
    }

    private static void savePlayerSnapshot(ServerPlayer player) {
        CompoundTag snap = new CompoundTag();
        snap.putBoolean("active", true);
        snap.putString("return_dim", player.level().dimension().identifier().toString());
        snap.putDouble("return_x", player.getX());
        snap.putDouble("return_y", player.getY());
        snap.putDouble("return_z", player.getZ());
        snap.putFloat("return_yaw", player.getYRot());
        snap.putFloat("return_pitch", player.getXRot());

        snap.putFloat("orig_health", player.getHealth());
        snap.putInt("orig_food", player.getFoodData().getFoodLevel());
        snap.putFloat("orig_sat", player.getFoodData().getSaturationLevel());
        snap.putInt("orig_xp_lvl", player.experienceLevel);
        snap.putFloat("orig_xp_prog", player.experienceProgress);
        snap.putInt("orig_total_xp", player.totalExperience);

        EntityData unitData = Load.Unit(player);
        RPGPlayerData rpgData = Load.playerRPGData(player);
        EntitySpellCap.ISpellsCap spellsCap = Load.spells(player);

        snap.put("unit_cap", unitData.saveToNBT().copy());
        snap.put("rpg_cap", rpgData.saveToNBT().copy());
        snap.put("spells_cap", spellsCap.saveToNBT().copy());

        int invSize = player.getInventory().getContainerSize();
        snap.putInt("inv_size", invSize);
        ListTag invList = new ListTag();
        List<ItemStack> memCopy = new ArrayList<>(invSize);

        for (int i = 0; i < invSize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            memCopy.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            if (!stack.isEmpty()) {
                CompoundTag itemNbt = StackNBT.saveStack(stack.copy());
                if (!itemNbt.isEmpty()) {
                    CompoundTag entry = new CompoundTag();
                    entry.putInt("slot", i);
                    entry.put("item", itemNbt);
                    invList.add(entry);
                }
            }
        }
        snap.put("inventory", invList);

        SERVER_SNAPSHOTS.put(player.getUUID(), snap);
        IN_MEMORY_INV_SNAPSHOTS.put(player.getUUID(), memCopy);
    }

    private static void restoreFromSnapshot(ServerPlayer player, CompoundTag snap, List<ItemStack> memInv) {
        String returnDimStr = snap.getStringOr("return_dim", "minecraft:overworld");
        double rx = snap.getDoubleOr("return_x", 0.5);
        double ry = snap.getDoubleOr("return_y", 64.0);
        double rz = snap.getDoubleOr("return_z", 0.5);

        restoreCapabilitiesAndInventory(player, snap, memInv);

        if (WorldUtils.isSimulationWorld(player.level())) {
            Identifier dimId = Identifier.tryParse(returnDimStr);
            if (dimId == null || dimId.equals(DimensionIds.SIMULATION_DIMENSION)) {
                dimId = Identifier.withDefaultNamespace("overworld");
            }
            TeleportUtils.teleport(player, BlockPos.containing(rx, ry, rz), dimId);
        }

        float origHealth = snap.getFloatOr("orig_health", player.getMaxHealth());
        player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), origHealth)));
        syncAll(player);
    }

    private static void restoreCapabilitiesAndInventory(ServerPlayer player, CompoundTag snap, List<ItemStack> memInv) {
        player.removeAllEffects();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.getInventory().clearContent();

        int invSize = player.getInventory().getContainerSize();
        if (memInv != null && !memInv.isEmpty()) {
            for (int i = 0; i < Math.min(invSize, memInv.size()); i++) {
                ItemStack st = memInv.get(i);
                player.getInventory().setItem(i, st == null || st.isEmpty() ? ItemStack.EMPTY : st.copy());
            }
        } else {
            ListTag invList = snap.getListOrEmpty("inventory");
            for (int i = 0; i < invList.size(); i++) {
                Tag t = invList.get(i);
                if (t instanceof CompoundTag entry) {
                    int slot = entry.getIntOr("slot", -1);
                    CompoundTag itemTag = entry.getCompoundOrEmpty("item");
                    if (slot >= 0 && slot < invSize && !itemTag.isEmpty()) {
                        ItemStack restored = StackNBT.loadStack(itemTag);
                        player.getInventory().setItem(slot, restored);
                    }
                }
            }
        }

        CompoundTag unitCapTag = snap.getCompoundOrEmpty("unit_cap");
        if (!unitCapTag.isEmpty()) {
            Load.Unit(player).loadFromNBT(unitCapTag.copy());
        }

        CompoundTag rpgCapTag = snap.getCompoundOrEmpty("rpg_cap");
        if (!rpgCapTag.isEmpty()) {
            Load.playerRPGData(player).loadFromNBT(rpgCapTag.copy());
        }

        CompoundTag spellsCapTag = snap.getCompoundOrEmpty("spells_cap");
        if (!spellsCapTag.isEmpty()) {
            Load.spells(player).loadFromNBT(spellsCapTag.copy());
        }

        player.experienceLevel = snap.getIntOr("orig_xp_lvl", 0);
        player.experienceProgress = snap.getFloatOr("orig_xp_prog", 0F);
        player.totalExperience = snap.getIntOr("orig_total_xp", 0);
        player.getFoodData().setFoodLevel(snap.getIntOr("orig_food", 20));
        player.getFoodData().setSaturation(snap.getFloatOr("orig_sat", 5F));

        EntityData unitData = Load.Unit(player);
        unitData.getStatusEffectsData().exileMap.clear();
        unitData.setEquipsChanged(true);
        unitData.tryRecalculateStats();

        float origHealth = snap.getFloatOr("orig_health", player.getMaxHealth());
        player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), origHealth)));

        syncAll(player);
    }

    public static boolean interceptLethalDamageInSimulation(ServerPlayer player, float vanillaDamage) {
        if (player == null || !WorldUtils.isSimulationWorld(player.level())) {
            return false;
        }
        if (player.getHealth() - vanillaDamage <= 0.5F) {
            float realDmg = HealthUtils.vanillaToReal(player, vanillaDamage);
            healPlayerFull(player);
            player.sendSystemMessage(Component.literal(
                "§c[Полигон] Смертельный удар (" + NumberUtils.format(realDmg) + " ед. урона) перехвачен! Персонаж мгновенно исцелён для продолжения теста."
            ));
            return true;
        }
        return false;
    }

    public static void setSimulationLevel(ServerPlayer player, int newLevel) {
        if (!ensureInSimulation(player)) {
            return;
        }
        int maxLvl = GameBalanceConfig.get() != null ? GameBalanceConfig.get().MAX_LEVEL : 50;
        int clamped = Math.max(1, Math.min(maxLvl, newLevel));

        EntityData unitData = Load.Unit(player);
        unitData.setLevel(clamped);
        unitData.setEquipsChanged(true);
        unitData.tryRecalculateStats();
        healPlayerFull(player);
        syncAll(player);

        player.sendSystemMessage(Component.literal("§a[Полигон] Уровень персонажа установлен на: §e" + clamped
            + " §7(Макс. Здоровье: §c" + NumberUtils.format(unitData.getUnit().healthData().getValue())
            + "§7, Мана: §b" + NumberUtils.format(unitData.getUnit().manaData().getValue())
            + "§7, Маг. щит: §d" + NumberUtils.format(unitData.getUnit().magicShieldData().getValue()) + "§7)"));
    }

    public static void grantBonusPoints(ServerPlayer player) {
        if (!ensureInSimulation(player)) {
            return;
        }
        RPGPlayerData rpgData = Load.playerRPGData(player);
        rpgData.talents.bonus_points += 50;
        rpgData.talents.reset_points = 999;

        EntityData unitData = Load.Unit(player);
        unitData.getCustomExactStats().addExactStat(
            "sim_bonus_all_stats",
            AllAttributes.getInstance().GUID(),
            50,
            ModType.FLAT
        );
        unitData.setEquipsChanged(true);
        unitData.tryRecalculateStats();
        healPlayerFull(player);
        syncAll(player);

        player.sendSystemMessage(Component.literal("§a[Полигон] Выдано +50 бонусных очков талантов, 999 очков сброса и +50 ко всем базовым характеристикам!"));
    }

    public static void resetBuildInSimulation(ServerPlayer player) {
        if (!ensureInSimulation(player)) {
            return;
        }
        RPGPlayerData rpgData = Load.playerRPGData(player);
        rpgData.talents = new TalentsData();
        rpgData.talents.reset_points = 999;
        rpgData.statPoints = new StatPointsData();

        Load.spells(player).getSpellsData().allocated_lvls.clear();
        Load.spells(player).getSpellsData().schools.clear();
        Load.spells(player).getSpellsData().hotbars.clear();

        EntityData unitData = Load.Unit(player);
        unitData.getCustomExactStats().removeExactStat("sim_bonus_all_stats");
        unitData.getStatusEffectsData().exileMap.clear();
        unitData.setEquipsChanged(true);
        unitData.tryRecalculateStats();
        healPlayerFull(player);
        syncAll(player);

        player.sendSystemMessage(Component.literal("§e[Полигон] Все таланты, очки характеристик и заклинания сброшены! Вы можете собрать новый билд с нуля."));
    }

    private record KitEntry(String slotHint, Item item) {
    }

    public static void giveGearKit(ServerPlayer player, int kitType) {
        if (!ensureInSimulation(player)) {
            return;
        }
        int lvl = Load.Unit(player).getLevel();
        String bestRarity = IRarity.EPIC_ID;
        int maxTier = -1;
        for (GearRarity r : ExileDB.GearRarities().getList()) {
            if (!r.is_unique_item && r.item_tier > maxTier) {
                maxTier = r.item_tier;
                bestRarity = r.GUID();
            }
        }

        List<KitEntry> entries = new ArrayList<>();
        String kitName;
        if (kitType == 0) {
            kitName = "Воин (Тяжёлая броня + Меч + Топор + Щит)";
            entries.add(new KitEntry("plate_helmet", Items.DIAMOND_HELMET));
            entries.add(new KitEntry("plate_chest", Items.DIAMOND_CHESTPLATE));
            entries.add(new KitEntry("plate_pants", Items.DIAMOND_LEGGINGS));
            entries.add(new KitEntry("plate_boots", Items.DIAMOND_BOOTS));
            entries.add(new KitEntry("sword", Items.DIAMOND_SWORD));
            entries.add(new KitEntry("axe", Items.DIAMOND_AXE));
            entries.add(new KitEntry("armor_shield", Items.SHIELD));
        } else if (kitType == 1) {
            kitName = "Охотник (Уклонение + Лук + Арбалет)";
            entries.add(new KitEntry("leather_helmet", Items.LEATHER_HELMET));
            entries.add(new KitEntry("leather_chest", Items.LEATHER_CHESTPLATE));
            entries.add(new KitEntry("leather_pants", Items.LEATHER_LEGGINGS));
            entries.add(new KitEntry("leather_boots", Items.LEATHER_BOOTS));
            entries.add(new KitEntry("bow", Items.BOW));
            entries.add(new KitEntry("crossbow", Items.CROSSBOW));
            entries.add(new KitEntry("dodge_shield", Items.SHIELD));
        } else if (kitType == 2) {
            kitName = "Маг (Магический щит + Посох + Скипетр)";
            Item staffItem = SlashItems.GearItems.STAFFS.containsKey(VanillaMaterial.DIAMOND)
                ? SlashItems.GearItems.STAFFS.get(VanillaMaterial.DIAMOND).get()
                : Items.STICK;
            entries.add(new KitEntry("cloth_helmet", Items.GOLDEN_HELMET));
            entries.add(new KitEntry("cloth_chest", Items.GOLDEN_CHESTPLATE));
            entries.add(new KitEntry("cloth_pants", Items.GOLDEN_LEGGINGS));
            entries.add(new KitEntry("cloth_boots", Items.GOLDEN_BOOTS));
            entries.add(new KitEntry("staff", staffItem));
            entries.add(new KitEntry("scepter", staffItem));
            entries.add(new KitEntry("ms_shield", Items.SHIELD));
        } else {
            kitName = "Бижутерия и Валюта (2 Кольца + Ожерелье + Души + Валюта)";
            Item ringItem = SlashItems.GearItems.RINGS.containsKey(VanillaMaterial.DIAMOND)
                ? SlashItems.GearItems.RINGS.get(VanillaMaterial.DIAMOND).get()
                : Items.GOLD_NUGGET;
            Item neckItem = SlashItems.GearItems.NECKLACES.containsKey(VanillaMaterial.DIAMOND)
                ? SlashItems.GearItems.NECKLACES.get(VanillaMaterial.DIAMOND).get()
                : Items.EMERALD;
            entries.add(new KitEntry("ring", ringItem));
            entries.add(new KitEntry("ring", ringItem));
            entries.add(new KitEntry("necklace", neckItem));
        }

        int given = 0;
        for (KitEntry entry : entries) {
            GearBlueprint bp = new GearBlueprint(lvl, 0);
            bp.item = entry.item();
            if (ExileDB.GearRarities().isRegistered(bestRarity)) {
                bp.rarity.set(ExileDB.GearRarities().get(bestRarity));
            }
            ExileDB.GearTypes().getList().stream()
                .filter(g -> g.GUID().contains(entry.slotHint()) || (g.gear_slot != null && g.gear_slot.contains(entry.slotHint())))
                .findFirst()
                .ifPresent(bp.gearItemSlot::set);
            ItemStack stack = bp.createStack();
            if (!stack.isEmpty()) {
                PlayerUtils.giveItem(stack, player);
                given++;
            }
        }

        if (kitType == 3) {
            for (Item currency : CurrencyItems.getAllCurrenciesFromRegistry()) {
                PlayerUtils.giveItem(new ItemStack(currency, 16), player);
            }
            ItemStack soul = StatSoulData.anySlotOfRarity(bestRarity).toStack();
            if (!soul.isEmpty()) {
                soul.setCount(8);
                PlayerUtils.giveItem(soul, player);
            }
        }

        Load.Unit(player).setEquipsChanged(true);
        Load.Unit(player).tryRecalculateStats();
        syncAll(player);

        player.sendSystemMessage(Component.literal("§a[Полигон] Выдан тестовый набор (Ур. " + lvl + "): §e" + kitName + " §7(предметов: " + given + ")"));
        if (kitType == 3) {
            player.sendSystemMessage(Component.literal("§7Подсказка: Ожерелье и до 2 колец автоматически работают прямо из вашего инвентаря!"));
        }
    }

    public static void spawnTargetDummy(ServerPlayer player, int level) {
        if (!ensureInSimulation(player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int maxLvl = GameBalanceConfig.get() != null ? GameBalanceConfig.get().MAX_LEVEL : 50;
        int lvl = Math.max(1, Math.min(maxLvl, level <= 0 ? Load.Unit(player).getLevel() : level));

        EntityType<?> golemType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("iron_golem"));
        Entity created = golemType != null ? golemType.create(serverLevel, EntitySpawnReason.COMMAND) : null;
        if (!(created instanceof Mob dummy)) {
            return;
        }
        BlockPos pos = DUMMY_POS;
        dummy.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180F, 0F);
        dummy.setNoAi(true);
        dummy.setPersistenceRequired();
        dummy.setSilent(true);

        EntityData data = Load.Unit(dummy);
        data.setType();
        data.setLevel(lvl);

        List<MobRarity> mobRarities = ExileDB.MobRarities().getList();
        MobRarity chosenRarity = ExileDB.MobRarities().isRegistered(IRarity.RARE_ID)
            ? ExileDB.MobRarities().get(IRarity.RARE_ID)
            : (!mobRarities.isEmpty() ? mobRarities.get(0) : null);
        if (chosenRarity != null) {
            data.setRarity(chosenRarity.GUID());
        }

        data.getCustomExactStats().addExactStat(
            "sim_dummy_hp",
            Health.getInstance().GUID(),
            500,
            ModType.PERCENT
        );
        data.setMobStats = true;
        data.setEquipsChanged(true);
        data.forceRecalculateStats();

        dummy.setHealth(dummy.getMaxHealth());
        dummy.setCustomName(Component.literal("§6[Манекен ДПС] §eУр. " + lvl));
        dummy.setCustomNameVisible(true);

        serverLevel.addFreshEntity(dummy);

        float realHp = data.getUnit().healthData().getValue();
        float armorVal = data.getUnit().armorData().getValue();
        float armorPct = Armor.getInstance().getUsableValue((int) armorVal, lvl) * 100F;

        player.sendSystemMessage(Component.literal(
            "§6[Полигон] Заспавнен Манекен ДПС (Ур. " + lvl + ")! §7[ХП: §c" + NumberUtils.format(realHp)
                + "§7, Броня: §e" + NumberUtils.format(armorVal) + " (" + NumberUtils.format(armorPct) + "% сниж.)§7]"
        ));
    }

    public static void spawnTestMob(ServerPlayer player, int level, int rarityRank) {
        if (!ensureInSimulation(player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int maxLvl = GameBalanceConfig.get() != null ? GameBalanceConfig.get().MAX_LEVEL : 50;
        int lvl = Math.max(1, Math.min(maxLvl, level <= 0 ? Load.Unit(player).getLevel() : level));

        Identifier mobTypeId = (rarityRank >= 3)
            ? Identifier.withDefaultNamespace("skeleton")
            : Identifier.withDefaultNamespace("zombie");
        EntityType<?> mobType = BuiltInRegistries.ENTITY_TYPE.getValue(mobTypeId);
        Entity created = mobType != null ? mobType.create(serverLevel, EntitySpawnReason.COMMAND) : null;
        if (!(created instanceof Mob mob)) {
            return;
        }

        BlockPos pos = DUMMY_POS;
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180F, 0F);
        mob.setPersistenceRequired();

        List<MobRarity> sortedRarities = new ArrayList<>(ExileDB.MobRarities().getList());
        sortedRarities.sort(Comparator.comparingDouble(MobRarity::ExtraHealthMulti));
        MobRarity chosenRarity = null;
        if (!sortedRarities.isEmpty()) {
            int idx = Math.max(0, Math.min(sortedRarities.size() - 1, rarityRank));
            chosenRarity = sortedRarities.get(idx);
        }

        EntityData data = Load.Unit(mob);
        data.setType();
        data.setLevel(lvl);
        if (chosenRarity != null) {
            data.setRarity(chosenRarity.GUID());
        }
        data.setMobStats = true;
        data.setEquipsChanged(true);
        data.forceRecalculateStats();
        mob.setHealth(mob.getMaxHealth());
        mob.setTarget(player);

        serverLevel.addFreshEntity(mob);

        float realHp = data.getUnit().healthData().getValue();
        float armorVal = data.getUnit().armorData().getValue();
        float armorPct = Armor.getInstance().getUsableValue((int) armorVal, lvl) * 100F;
        float eleResVal = data.getUnit().getCalculatedStat(new ElementalResist(Elements.Fire)).getValue();
        float eleResPct = new ElementalResist(Elements.Fire).getUsableValue((int) eleResVal, lvl) * 100F;

        String rarName = chosenRarity != null ? chosenRarity.locName().getString() : "Обычный";
        player.sendSystemMessage(Component.literal(
            "§c[Полигон] Заспавнен тестовый моб (§e" + rarName + "§c, Ур. " + lvl + ")! "
                + "§7[ХП: §c" + NumberUtils.format(realHp)
                + "§7 | Броня: §e" + NumberUtils.format(armorVal) + " (" + NumberUtils.format(armorPct) + "%)"
                + "§7 | Сопр. стихиям: §b" + NumberUtils.format(eleResVal) + " (" + NumberUtils.format(eleResPct) + "%)§7]"
        ));
    }

    public static void clearSimulationMobs(ServerPlayer player, boolean notify) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB box = new AABB(player.blockPosition()).inflate(80);
        List<Mob> mobs = serverLevel.getEntitiesOfClass(Mob.class, box, m -> true);
        int count = 0;
        for (Mob m : mobs) {
            m.discard();
            count++;
        }
        if (notify) {
            player.sendSystemMessage(Component.literal("§e[Полигон] Удалено тестовых существ/манекенов: " + count));
        }
    }

    public static void runDefenseStressTest(ServerPlayer player, int attackerLevel, int mode) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int maxLvl = GameBalanceConfig.get() != null ? GameBalanceConfig.get().MAX_LEVEL : 50;
        int lvl = Math.max(1, Math.min(maxLvl, attackerLevel <= 0 ? Load.Unit(player).getLevel() : attackerLevel));

        EntityData playerData = Load.Unit(player);
        playerData.setEquipsChanged(true);
        playerData.tryRecalculateStats();
        healPlayerFull(player);

        EntityType<?> zombieType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
        Entity created = zombieType != null ? zombieType.create(serverLevel, EntitySpawnReason.COMMAND) : null;
        if (!(created instanceof Mob simAttacker)) {
            return;
        }
        EntityData attackerData = Load.Unit(simAttacker);
        attackerData.setType();
        attackerData.setLevel(lvl);
        attackerData.setMobStats = true;
        attackerData.forceRecalculateStats();

        AttackType attackType = (mode == 2) ? AttackType.spell : AttackType.attack;
        WeaponTypes weaponType = (mode == 1) ? WeaponTypes.bow : (mode == 2 ? WeaponTypes.staff : WeaponTypes.sword);
        PlayStyle playStyle = (mode == 1) ? PlayStyle.ranged : (mode == 2 ? PlayStyle.magic : PlayStyle.melee);
        Elements element = (mode == 2) ? Elements.Fire : Elements.Physical;

        float baseScale = StatScaling.NORMAL.scale(8.0F, lvl);

        int totalHits = 20;
        int dodgedCount = 0;
        float totalRawDmg = 0F;
        float totalFinalDmg = 0F;
        float totalShieldAbsorbed = 0F;
        float totalManaAbsorbed = 0F;

        for (int i = 0; i < totalHits; i++) {
            healPlayerFull(player);
            float msBefore = playerData.getResources().getShield();
            float manaBefore = playerData.getResources().getMana();

            DamageEvent dmgEvent = EventBuilder.ofDamage(null, simAttacker, player, baseScale)
                .setupDamage(attackType, weaponType, playStyle)
                .set(x -> {
                    x.setElement(element);
                    if (mode == 2) {
                        x.data.setString(EventData.SPELL, "sim_spell_test");
                    }
                })
                .build();

            dmgEvent.initBeforeActivating();
            dmgEvent.calculateEffects();

            totalRawDmg += baseScale;
            if (dmgEvent.data.isDodged()) {
                dodgedCount++;
            } else {
                float actual = Math.max(0F, dmgEvent.data.getNumber(EventData.NUMBER).number);
                totalFinalDmg += actual;
                float msAfter = playerData.getResources().getShield();
                float manaAfter = playerData.getResources().getMana();
                if (msBefore > msAfter) {
                    totalShieldAbsorbed += (msBefore - msAfter);
                }
                if (manaBefore > manaAfter) {
                    totalManaAbsorbed += (manaBefore - manaAfter);
                }
            }
        }

        healPlayerFull(player);

        float dodgeRating = playerData.getUnit().dodgeData().getValue();
        float dodgePct = DodgeRating.getInstance().getUsableValue((int) dodgeRating, lvl) * 100F;
        float spellDodgeRating = playerData.getUnit().getCalculatedStat(SpellDodge.getInstance()).getValue();
        float spellDodgePct = SpellDodge.getInstance().getUsableValue((int) spellDodgeRating, lvl) * 100F;
        float armorRating = playerData.getUnit().armorData().getValue();
        float armorPct = Armor.getInstance().getUsableValue((int) armorRating, lvl) * 100F;
        float fireResRating = playerData.getUnit().getCalculatedStat(new ElementalResist(Elements.Fire)).getValue();
        float fireResPct = new ElementalResist(Elements.Fire).getUsableValue((int) fireResRating, lvl) * 100F;

        String modeLabel = (mode == 0) ? "Физический ближний бой" : (mode == 1 ? "Дальний бой (снаряд)" : "Огненное заклинание");

        player.sendSystemMessage(Component.literal("══════════════════════════════════════════").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("🛡 ТЕСТ ЗАЩИТЫ И УКЛОНЕНИЯ (20 ударов моба Ур. " + lvl + ")").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("• Тип атаки: §f" + modeLabel + " §7(базовый урон удара: §e" + NumberUtils.format(baseScale) + "§7)"));
        player.sendSystemMessage(Component.literal("• Ваши защитные статы против Ур. " + lvl + ":"));
        player.sendSystemMessage(Component.literal("  - Уклонение от атак: §a" + NumberUtils.format(dodgeRating) + " §7(шанс: §a" + NumberUtils.format(dodgePct) + "%§7)"
            + " | Уклонение от чар: §b" + NumberUtils.format(spellDodgeRating) + " §7(шанс: §b" + NumberUtils.format(spellDodgePct) + "%§7)"));
        player.sendSystemMessage(Component.literal("  - Броня: §e" + NumberUtils.format(armorRating) + " §7(снижение физ. урона: §e" + NumberUtils.format(armorPct) + "%§7)"
            + " | Сопр. огню: §6" + NumberUtils.format(fireResRating) + " §7(снижение: §6" + NumberUtils.format(fireResPct) + "%§7)"));
        player.sendSystemMessage(Component.literal("• Результат серии из 20 ударов:"));
        player.sendSystemMessage(Component.literal("  - Успешных уклонений: §a" + dodgedCount + " / " + totalHits + " (" + (dodgedCount * 100 / totalHits) + "%)"));
        player.sendSystemMessage(Component.literal("  - Суммарный входящий урон до защиты: §7" + NumberUtils.format(totalRawDmg)));
        player.sendSystemMessage(Component.literal("  - Поглощено Магическим щитом: §d" + NumberUtils.format(totalShieldAbsorbed)
            + " §7| Поглощено Маной: §b" + NumberUtils.format(totalManaAbsorbed)));
        player.sendSystemMessage(Component.literal("  - Итоговый урон по Здоровью после брони/резистов/щитов: §c" + NumberUtils.format(totalFinalDmg)
            + " §7(в среднем §c" + NumberUtils.format(totalFinalDmg / totalHits) + " §7за удар)"));
        player.sendSystemMessage(Component.literal("══════════════════════════════════════════").withStyle(ChatFormatting.AQUA));
    }

    public static void printMobScalingTable(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int[] levels = new int[]{1, 5, 10, 20, 30, 40, 50};
        List<MobRarity> sorted = new ArrayList<>(ExileDB.MobRarities().getList());
        sorted.sort(Comparator.comparingDouble(MobRarity::ExtraHealthMulti));

        MobRarity common = !sorted.isEmpty() ? sorted.get(0) : null;
        MobRarity rare = sorted.size() > 2 ? sorted.get(2) : common;
        MobRarity mythic = !sorted.isEmpty() ? sorted.get(sorted.size() - 1) : common;

        float commonHpMulti = common != null ? common.ExtraHealthMulti() : 1.0F;
        float rareHpMulti = rare != null ? rare.ExtraHealthMulti() : 2.5F;
        float mythicHpMulti = mythic != null ? mythic.ExtraHealthMulti() : 6.0F;
        float commonStatMulti = common != null ? common.StatMultiplier() : 1.0F;
        float mythicStatMulti = mythic != null ? mythic.StatMultiplier() : 3.0F;

        player.sendSystemMessage(Component.literal("══════════════════════════════════════════").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("📊 ТАБЛИЦА СКАЛИРОВАНИЯ ХП И СТАТОВ МОБОВ (База: 20 ХП ваниллы)").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        for (int lvl : levels) {
            float extraFactor = 1.0F + (0.02F * lvl);
            float baseHpAt1 = 20.0F * extraFactor;
            float scaledCommonHp = Health.getInstance().scale(ModType.FLAT, baseHpAt1 * commonHpMulti, lvl);
            float scaledRareHp = Health.getInstance().scale(ModType.FLAT, baseHpAt1 * rareHpMulti, lvl);
            float scaledMythicHp = Health.getInstance().scale(ModType.FLAT, baseHpAt1 * mythicHpMulti, lvl);

            float commonArmor = StatScaling.NORMAL.scale(10.0F * commonStatMulti, lvl);
            float mythicArmor = StatScaling.NORMAL.scale(10.0F * mythicStatMulti, lvl);
            float commonDmg = StatScaling.NORMAL.scale(4.0F * commonStatMulti, lvl);
            float mythicDmg = StatScaling.NORMAL.scale(4.0F * mythicStatMulti, lvl);

            player.sendSystemMessage(Component.literal(
                "§eУр. " + String.format("%2d", lvl)
                    + " §7| ХП [Обыч: §a" + (int) scaledCommonHp
                    + "§7, Редк: §b" + (int) scaledRareHp
                    + "§7, Высш: §d" + (int) scaledMythicHp
                    + "§7] | Броня: §e" + (int) commonArmor + "–" + (int) mythicArmor
                    + " §7| Баз. урон: §c" + NumberUtils.format(commonDmg) + "–" + NumberUtils.format(mythicDmg)
            ));
        }
        player.sendSystemMessage(Component.literal("══════════════════════════════════════════").withStyle(ChatFormatting.GOLD));
    }

    public static void runFullAudit(ServerPlayer player) {
        if (player == null) {
            return;
        }
        EntityData unitData = Load.Unit(player);
        RPGPlayerData rpgData = Load.playerRPGData(player);
        unitData.setEquipsChanged(true);
        unitData.tryRecalculateStats();

        List<Perk> allPerks = ExileDB.Perks().getList();
        int totalPerks = allPerks.size();
        int validPerks = 0;
        int gameChangers = 0;
        int scaledFlatDefPerks = 0;

        for (Perk perk : allPerks) {
            if (perk != null && perk.stats != null && !perk.stats.isEmpty()) {
                boolean ok = true;
                for (OptScaleExactStat opt : perk.stats) {
                    if (opt == null || opt.getStat() == null) {
                        ok = false;
                        break;
                    }
                    if (opt.scale_to_lvl) {
                        scaledFlatDefPerks++;
                    }
                }
                if (ok) {
                    validPerks++;
                }
                if (perk.getType() == Perk.PerkType.MAJOR) {
                    gameChangers++;
                }
            }
        }

        int allocatedCount = 0;
        for (TalentTree tree : ExileDB.TalentTrees().getList()) {
            allocatedCount += rpgData.talents.getSchool(tree).getAllocatedPointsInSchool(tree);
        }

        List<ItemStack> trinkets = MyCurioUtils.getAllSlots(player);
        int activeTrinkets = 0;
        for (ItemStack st : trinkets) {
            if (!st.isEmpty()) {
                activeTrinkets++;
            }
        }

        int lvl = unitData.getLevel();
        Unit u = unitData.getUnit();
        float hp = u.healthData().getValue();
        float mana = u.manaData().getValue();
        float ms = u.magicShieldData().getValue();
        float armor = u.armorData().getValue();
        float armorPct = Armor.getInstance().getUsableValue((int) armor, lvl) * 100F;
        float dodge = u.dodgeData().getValue();
        float dodgePct = DodgeRating.getInstance().getUsableValue((int) dodge, lvl) * 100F;
        float spellDodge = u.getCalculatedStat(SpellDodge.getInstance()).getValue();
        float spellDodgePct = SpellDodge.getInstance().getUsableValue((int) spellDodge, lvl) * 100F;

        player.sendSystemMessage(Component.literal("══════════════════════════════════════════").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("✔ АУДИТ ТАЛАНТОВ, ХАРАКТЕРИСТИК И ЭКИПИРОВКИ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("• Зарегистрировано перков в БД: §a" + validPerks + " / " + totalPerks
            + " §7(Ключевых талантов: §d" + gameChangers + "§7, со скалированием по уровню: §e" + scaledFlatDefPerks + "§7)"));
        player.sendSystemMessage(Component.literal("• Изучено талантов у персонажа: §e" + allocatedCount
            + " §7| Активных украшений (кольца/ожерелье): §b" + activeTrinkets + " / 3"));
        player.sendSystemMessage(Component.literal("• Ресурсы (Ур. " + lvl + "): §cХП " + NumberUtils.format(hp)
            + " §7| §bМана " + NumberUtils.format(mana)
            + " §7| §dМаг. щит " + NumberUtils.format(ms)));
        player.sendSystemMessage(Component.literal("• Защита (Ур. " + lvl + "): §eБроня " + NumberUtils.format(armor) + " (" + NumberUtils.format(armorPct) + "%)"
            + " §7| §aУклонение " + NumberUtils.format(dodge) + " (" + NumberUtils.format(dodgePct) + "%)"
            + " §7| §bУкл. от чар " + NumberUtils.format(spellDodge) + " (" + NumberUtils.format(spellDodgePct) + "%)"));
        player.sendSystemMessage(Component.literal("══════════════════════════════════════════").withStyle(ChatFormatting.GREEN));
    }

    public static void healPlayerFull(ServerPlayer player) {
        if (player == null) {
            return;
        }
        EntityData unitData = Load.Unit(player);
        float maxHp = unitData.getUnit().healthData().getValue();
        float maxMana = unitData.getUnit().manaData().getValue();

        EventBuilder.ofRestore(player, player, ResourceType.health, RestoreType.heal, Math.max(1000F, maxHp)).build().Activate();
        EventBuilder.ofRestore(player, player, ResourceType.mana, RestoreType.heal, Math.max(1000F, maxMana)).build().Activate();
        EventBuilder.ofRestore(player, player, ResourceType.energy, RestoreType.heal, 1000F).build().Activate();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20F);
    }

    public static void syncAll(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Load.Unit(player).syncToClient(player);
        Load.playerRPGData(player).syncToClient(player);
        Load.spells(player).syncToClient(player);
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean ensureInSimulation(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!WorldUtils.isSimulationWorld(player.level())) {
            enterSimulation(player);
        }
        return true;
    }

    private static ServerLevel getSimulationLevel(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) {
            return null;
        }
        for (ServerLevel lvl : player.level().getServer().getAllLevels()) {
            if (lvl.dimension().identifier().equals(DimensionIds.SIMULATION_DIMENSION)) {
                return lvl;
            }
        }
        return null;
    }

    public static void buildArena(ServerLevel level) {
        if (level == null) {
            return;
        }
        int cx = ARENA_CENTER.getX();
        int cy = ARENA_CENTER.getY() - 1;
        int cz = ARENA_CENTER.getZ();
        int radius = 16;

        BlockState floorBase = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState ring5 = Blocks.QUARTZ_BRICKS.defaultBlockState();
        BlockState ring10 = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        BlockState border = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
        BlockState light = Blocks.SEA_LANTERN.defaultBlockState();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos floorPos = new BlockPos(cx + dx, cy, cz + dz);
                int distSq = dx * dx + dz * dz;
                if (distSq >= 23 && distSq <= 27) {
                    level.setBlock(floorPos, ring5, 2);
                } else if (distSq >= 96 && distSq <= 104) {
                    level.setBlock(floorPos, ring10, 2);
                } else if ((Math.abs(dx) == 8 && Math.abs(dz) == 8) || (dx == 0 && dz == 0)) {
                    level.setBlock(floorPos, light, 2);
                } else {
                    level.setBlock(floorPos, floorBase, 2);
                }

                for (int y = 1; y <= 6; y++) {
                    BlockPos airPos = floorPos.above(y);
                    if (!level.getBlockState(airPos).isAir()) {
                        level.setBlock(airPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }

                if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                    level.setBlock(floorPos.above(), border, 2);
                    if (Math.abs(dx) % 8 == 0 && Math.abs(dz) % 8 == 0) {
                        level.setBlock(floorPos.above(2), light, 2);
                    }
                }
            }
        }

        // Pedestal for Target Dummy / Test Mob at (0, 63, 8)
        level.setBlock(DUMMY_POS.below(), Blocks.GOLD_BLOCK.defaultBlockState(), 2);

        // Return Portal at (0, 64, -8)
        level.setBlock(EXIT_PORTAL_POS.below(), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(EXIT_PORTAL_POS, SlashBlocks.PORTAL.get().defaultBlockState(), 2);
        level.setBlock(EXIT_PORTAL_POS.west(), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
        level.setBlock(EXIT_PORTAL_POS.west().above(), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
        level.setBlock(EXIT_PORTAL_POS.east(), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
        level.setBlock(EXIT_PORTAL_POS.east().above(), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
        level.setBlock(EXIT_PORTAL_POS.above(2), Blocks.SEA_LANTERN.defaultBlockState(), 2);
    }
}
