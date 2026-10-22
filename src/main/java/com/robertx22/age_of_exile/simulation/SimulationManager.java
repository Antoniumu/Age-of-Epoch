package com.robertx22.age_of_exile.simulation;

import net.minecraft.world.entity.EntityTypes;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.gear_slots.GearSlot;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.Armor;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.DodgeRating;
import com.robertx22.mine_and_slash.database.data.stats.types.generated.ElementalResist;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.energy.Energy;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield.MagicShield;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.dimension.DimensionIds;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulData;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourcesData;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.StackNBT;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SimulationManager {

    private static final Map<UUID, CompoundTag> SNAPSHOTS = new ConcurrentHashMap<>();

    private SimulationManager() {}

    public static boolean isSimulationLevel(LevelAccessor world) {
        if (world instanceof Level lvl) {
            return lvl.dimension().identifier().equals(DimensionIds.SIMULATION_DIMENSION);
        }
        if (world instanceof net.minecraft.world.level.ServerLevelAccessor sla && sla.getLevel() != null) {
            return sla.getLevel().dimension().identifier().equals(DimensionIds.SIMULATION_DIMENSION);
        }
        return false;
    }

    public static boolean isInSimulation(ServerPlayer player) {
        return player != null && (SNAPSHOTS.containsKey(player.getUUID()) || isSimulationLevel(player.level()));
    }

    public static CompoundTag getSnapshotForSave(Entity entity) {
        return entity instanceof ServerPlayer sp ? SNAPSHOTS.get(sp.getUUID()) : null;
    }

    public static void loadSnapshotFromSave(Entity entity, CompoundTag snap) {
        if (entity instanceof ServerPlayer sp && snap != null && !snap.isEmpty()) {
            SNAPSHOTS.put(sp.getUUID(), snap.copy());
        }
    }

    public static void enterSimulation(ServerPlayer player) {
        if (SNAPSHOTS.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Вы уже находитесь в режиме Симуляции Билда.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        CompoundTag snap = new CompoundTag();
        snap.put("unit_cap", Load.Unit(player).saveToNBT());
        snap.put("rpg_cap", Load.player(player).saveToNBT());
        ListTag inv = new ListTag();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty()) {
                CompoundTag slotTag = StackNBT.saveStack(st.copy());
                slotTag.putInt("SlotIdx", i);
                inv.add(slotTag);
            }
        }
        snap.put("inv", inv);
        SNAPSHOTS.put(player.getUUID(), snap);
        player.sendSystemMessage(Component.literal("★ Режим Симуляции активирован! Весь ваш прогресс сохранён в снимке.").withStyle(ChatFormatting.GREEN));
    }

    public static void exitSimulation(ServerPlayer player) {
        CompoundTag snap = SNAPSHOTS.remove(player.getUUID());
        if (snap == null) {
            player.sendSystemMessage(Component.literal("Снимок симуляции не активен.").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.getInventory().clearContent();
        ListTag inv = snap.getListOrEmpty("inv");
        for (int i = 0; i < inv.size(); i++) {
            CompoundTag slotTag = inv.getCompoundOrEmpty(i);
            int idx = slotTag.getIntOr("SlotIdx", -1);
            if (idx >= 0 && idx < player.getInventory().getContainerSize()) {
                player.getInventory().setItem(idx, StackNBT.loadStack(slotTag));
            }
        }
        Load.Unit(player).loadFromNBT(snap.getCompoundOrEmpty("unit_cap"));
        Load.player(player).loadFromNBT(snap.getCompoundOrEmpty("rpg_cap"));
        Load.Unit(player).forceRecalculateStats();
        Load.Unit(player).syncToClient(player);
        Load.player(player).syncToClient(player);
        healPlayerFull(player);
        player.sendSystemMessage(Component.literal("★ Вы вышли из Симуляции — исходный персонаж и инвентарь восстановлены!").withStyle(ChatFormatting.AQUA));
    }

    public static void healPlayerFull(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        EntityData ud = Load.Unit(player);
        Unit u = ud.getUnit();
        ud.getResources().restore(player, ResourceType.health, u.getCalculatedStat(Health.getInstance()).getValue());
        ud.getResources().restore(player, ResourceType.magic_shield, u.getCalculatedStat(MagicShield.getInstance()).getValue());
        ud.getResources().restore(player, ResourceType.energy, u.getCalculatedStat(Energy.getInstance()).getValue());
    }

    public static boolean interceptLethalDamageInSimulation(ServerPlayer player, float vanillaDamage) {
        if (!isInSimulation(player)) return false;
        if (player.getHealth() - vanillaDamage <= 0.5F) {
            float realDmg = HealthUtils.vanillaToReal(player, vanillaDamage);
            healPlayerFull(player);
            player.sendSystemMessage(Component.literal(String.format("[Симуляция] Перехвачен смертельный удар (%.0f ед. урона) — здоровье восстановлено!", realDmg))
                .withStyle(ChatFormatting.GOLD));
            return true;
        }
        return false;
    }

    public static void setLevel(ServerPlayer player, int targetLevel) {
        if (!isInSimulation(player)) enterSimulation(player);
        int clamped = Math.max(1, Math.min(GameBalanceConfig.get().MAX_LEVEL, targetLevel));
        EntityData ud = Load.Unit(player);
        ud.setLevel(clamped);
        ud.forceRecalculateStats();
        healPlayerFull(player);
        ud.syncToClient(player);
        Load.player(player).syncToClient(player);
        player.sendSystemMessage(Component.literal("[Симуляция] Уровень установлен: " + clamped).withStyle(ChatFormatting.GREEN));
    }

    public static void resetAllAllocations(ServerPlayer player) {
        if (!isInSimulation(player)) enterSimulation(player);
        PlayerData pd = Load.player(player);
        pd.statPoints.reset();
        pd.talents = new com.robertx22.mine_and_slash.saveclasses.perks.TalentsData();
        pd.ascClass.allocated_lvls.clear();
        EntityData ud = Load.Unit(player);
        ud.forceRecalculateStats();
        pd.syncToClient(player);
        ud.syncToClient(player);
        player.sendSystemMessage(Component.literal("[Симуляция] Все очки статов, талантов и заклинаний сброшены.").withStyle(ChatFormatting.YELLOW));
    }

    public static void grantTestKit(ServerPlayer player) {
        if (!isInSimulation(player)) enterSimulation(player);
        int lvl = Load.Unit(player).getLevel();
        LootInfo info = LootInfo.ofLevel(lvl);
        GearRarity topRar = ExileDB.GearRarities().getList().stream()
            .max((a, b) -> Integer.compare(a.item_tier, b.item_tier)).orElse(null);

        for (GearSlot slot : ExileDB.GearSlots().getList()) {
            GearBlueprint bp = new GearBlueprint(info);
            if (topRar != null) bp.rarity.set(topRar);
            ItemStack gear = bp.createStack();
            if (!gear.isEmpty()) player.getInventory().add(gear);

            StatSoulData soul = new StatSoulData();
            soul.slot = slot.GUID();
            soul.rar = topRar != null ? topRar.GUID() : IRarity.UNIQUE_ID;
            player.getInventory().add(soul.toStack());
        }
        for (Item orb : ExileCurrency.CACHED_MAP.get().keySet()) {
            player.getInventory().add(new ItemStack(orb, 16));
        }
        player.sendSystemMessage(Component.literal("[Симуляция] Выдан полный набор экипировки, душ статов и сфер крафта!").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static void spawnDummy(ServerPlayer player, boolean boss) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        Zombie dummy = (Zombie) EntityTypes.ZOMBIE.create(sl, EntitySpawnReason.COMMAND);
        if (dummy == null) return;
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 3);
        dummy.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        dummy.setNoAi(true);
        dummy.setCustomName(Component.literal(boss ? "§c[Манекен Босса]" : "§e[Тренировочный Манекен]"));
        dummy.setCustomNameVisible(true);
        sl.addFreshEntity(dummy);

        EntityData data = Load.Unit(dummy);
        data.setLevel(Load.Unit(player).getLevel());
        if (boss) data.setRarity(IRarity.BOSS_ID);
        data.setMobStats = true;
        data.forceRecalculateStats();
        dummy.setHealth(dummy.getMaxHealth());

        int lvl = data.getLevel();
        Unit u = data.getUnit();
        float armorVal = u.getCalculatedStat(Armor.getInstance()).getValue();
        float armorPct = Armor.getInstance().getUsableValue(u, (int) armorVal, lvl) * 100F;
        float resVal = u.getCalculatedStat(new ElementalResist(Elements.Fire)).getValue();
        float resPct = new ElementalResist(Elements.Fire).getUsableValue(u, (int) resVal, lvl) * 100F;
        player.sendSystemMessage(Component.literal(String.format(
            "[Симуляция] Спавн манекена ур. %d | Броня: %.0f (%.1f%%) | Сопр. стихиям: %.0f (%.1f%%)",
            lvl, armorVal, armorPct, resVal, resPct)).withStyle(ChatFormatting.AQUA));
    }

    public static void testIncomingHit(ServerPlayer player, int mode) {
        EntityData pd = Load.Unit(player);
        pd.forceRecalculateStats();
        Unit u = pd.getUnit();
        int lvl = pd.getLevel();
        float armor = u.getCalculatedStat(Armor.getInstance()).getValue();
        float armorMit = Armor.getInstance().getUsableValue(u, (int) armor, lvl);
        float dodge = u.getCalculatedStat(DodgeRating.getInstance()).getValue();
        float dodgeChance = DodgeRating.getInstance().getUsableValue(u, (int) dodge, lvl);
        float fireRes = new ElementalResist(Elements.Fire).getUsableValue(u, (int) u.getCalculatedStat(new ElementalResist(Elements.Fire)).getValue(), lvl);
        float raw = 100F + lvl * 25F;
        float mitigated = mode == 2 ? raw * (1F - fireRes) : raw * (1F - armorMit);
        player.sendSystemMessage(Component.literal(String.format(
            "[Симуляция] Тест удара (%s, ур. %d): сырой %.0f -> после защиты %.0f (Броня %.1f%% | Уклон %.1f%% | Сопр. %.1f%%)",
            mode == 2 ? "Заклинание" : "Атака", lvl, raw, mitigated, armorMit * 100F, dodgeChance * 100F, fireRes * 100F
        )).withStyle(ChatFormatting.GOLD));
    }
}
