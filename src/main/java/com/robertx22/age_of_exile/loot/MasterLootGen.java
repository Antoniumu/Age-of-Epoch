package com.robertx22.age_of_exile.loot;

import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.loot.generators.*;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ItemUtils;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MasterLootGen {

    public static List<ItemStack> generateLoot(LootInfo info) {
        List<ItemStack> items = new ArrayList<>();
        try {

            if (info == null) {
                return items;
            }

            items = populateOnce(info);

            int tries = 0;

            while (items.size() < info.getMinItems()) {

                tries++;
                if (tries > 20) {
                    System.out.println("Tried to generate loot many times but failed! " + info.toString());
                    break;
                }
                List<ItemStack> extra = populateOnce(info);
                if (!extra.isEmpty()) {
                    items.add(RandomUtils.randomFromList(extra));
                }
            }

            tries = 0;

            while (items.size() > info.getMaxItems()) {
                tries++;
                if (tries > 50) {
                    System.out.println("Took too many tries to remove items from masterlootgen");
                    break;
                }
                items.remove(RandomUtils.RandomRange(0, items.size() - 1));
            }

            if (info.rpgData != null && info.favorRank != null) {
                info.rpgData.favor.afterLootingItems(info.player, info.favorRank.favor_drain_per_item, info, items.size());

                List<ItemStack> extraFavorItems = new ArrayList<ItemStack>();

                int extraTries = 0;

                while (extraFavorItems.size() < info.getExtraFavorItems()) {

                    extraTries++;
                    if (extraTries > 20) {
                        System.out.println("Tried to generate loot many times but failed! ");
                        break;
                    }
                    List<ItemStack> extra = populateOnce(info);
                    if (!extra.isEmpty()) {
                        extraFavorItems.add(RandomUtils.randomFromList(extra));
                    }
                }

                info.rpgData.favor.afterLootingItems(info.player, info.favorRank.extra_item_favor_cost, info, extraFavorItems.size());

                items.addAll(extraFavorItems);

            }

            // Pity: если сухая серия слишком длинная, руна/самоцвет приходит принудительно.
            applyPity(info, items);

            // Кодекс: запоминаем собранные уникаты (и с мобов, и из сундуков).
            trackCodex(info, items);

            items.forEach(x -> {
                ItemUtils.tryAnnounceItem(x, info.player);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    /**
     * Pity для рун и самоцветов: шанс растёт с числом убийств без дропа, поэтому
     * длинные «сухие» серии (руны — 0.05% базового шанса) срезаются.
     */
    private static void applyPity(LootInfo info, List<ItemStack> items) {
        if (info == null || items == null || info.lootOrigin != LootInfo.LootOrigin.MOB) {
            return;
        }
        if (!(info.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        try {
            com.robertx22.age_of_exile.gamemaster.GameMaster gm =
                com.robertx22.age_of_exile.gamemaster.GameMaster.get();

            boolean hasRune = items.stream()
                .anyMatch(MasterLootGen::isRune);
            boolean hasGem = items.stream()
                .anyMatch(MasterLootGen::isGem);

            if (hasRune) {
                gm.onRuneDropped(serverPlayer);
            } else {
                double chance = gm.runePityChance(serverPlayer);
                if (chance > 0 && Math.random() < chance) {
                    ItemStack rune = new RuneLootGen(info).generateForced();
                    if (rune != null) {
                        items.add(rune);
                        gm.onRuneDropped(serverPlayer);
                        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component
                            .literal("GameMaster: долгая серия без руны — руна гарантирована.")
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
                    }
                }
            }

            if (hasGem) {
                gm.onGemDropped(serverPlayer);
            } else {
                double chance = gm.gemPityChance(serverPlayer);
                if (chance > 0 && Math.random() < chance) {
                    ItemStack gem = new GemLootGen(info).generateForced();
                    if (gem != null) {
                        items.add(gem);
                        gm.onGemDropped(serverPlayer);
                    }
                }
            }
        } catch (Exception ignored) {
            // pity не должен ломать выдачу лута
        }
    }

    private static boolean isRune(ItemStack stack) {
        return crateType(stack) == com.robertx22.age_of_exile.uncommon.enumclasses.LootType.Rune;
    }

    private static boolean isGem(ItemStack stack) {
        return crateType(stack) == com.robertx22.age_of_exile.uncommon.enumclasses.LootType.Gem;
    }

    /** Тип содержимого лут-сундука (или null, если это не сундук). */
    private static com.robertx22.age_of_exile.uncommon.enumclasses.LootType crateType(ItemStack stack) {
        try {
            if (!(stack.getItem() instanceof com.robertx22.age_of_exile.vanilla_mc.items.crates.gem_crate.LootCrateItem crate)) {
                return null;
            }
            com.robertx22.age_of_exile.vanilla_mc.items.crates.gem_crate.LootCrateData data = crate.getData(stack);
            return data == null ? null : data.type;
        } catch (Exception e) {
            return null;
        }
    }

    /** Кодекс: уникаты из любого источника (моб, сундук, лут-сундук). */
    private static void trackCodex(LootInfo info, List<ItemStack> items) {
        if (info == null || items == null || !(info.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        try {
            com.robertx22.age_of_exile.gamemaster.GameMaster gm =
                com.robertx22.age_of_exile.gamemaster.GameMaster.get();
            if (!gm.config()
                .codex_enabled) {
                return;
            }
            for (ItemStack stack : items) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                // Гир падает ДУШАМИ, поэтому сначала смотрим в душу: раньше кодекс
                // проверял только Gear.has() и не видел ни одного униката с моба/сундука.
                if (com.robertx22.age_of_exile.uncommon.datasaving.StackSaving.STAT_SOULS.has(stack)) {
                    com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData soul =
                        com.robertx22.age_of_exile.uncommon.datasaving.StackSaving.STAT_SOULS.loadFrom(stack);
                    if (soul != null && soul.uniq != null && !soul.uniq.isEmpty()) {
                        gm.recordCodex(serverPlayer, soul.uniq);
                    }
                    continue;
                }

                if (!Gear.has(stack)) {
                    continue;
                }
                com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData data = Gear.Load(stack);
                if (data == null || data.getRarity() == null || !data.getRarity().is_unique_item) {
                    continue;
                }
                String id = data.uniq_id == null || data.uniq_id.isEmpty()
                    ? (data.item_id == null || data.item_id.isEmpty()
                    ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
                    : data.item_id)
                    : data.uniq_id;
                gm.recordCodex(serverPlayer, id);
            }
        } catch (Exception ignored) {
        }
    }

    private static List<ItemStack> populateOnce(LootInfo info) {
        List<ItemStack> items = new ArrayList<ItemStack>();

        if (info == null) {
            return items;
        }

        try {
            items.addAll(new CurrencyLootGen(info).tryGenerate());
            items.addAll(new GearSoulLootGen(info).tryGenerate());
            items.addAll(new GemLootGen(info).tryGenerate());
            items.addAll(new RuneLootGen(info).tryGenerate());
            items.addAll(new IngredientLootGen(info).tryGenerate());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items.stream()
            .filter(x -> x != null && !x.isEmpty())
            .collect(Collectors.toList());
    }

    public static List<ItemStack> generateLoot(LivingEntity victim, Player killer) {

        LootInfo info = LootInfo.ofMobKilled(killer, victim);
        List<ItemStack> items = generateLoot(info);

        return items;
    }

    public static void genAndDrop(LivingEntity victim, Player killer) {
        List<ItemStack> items = generateLoot(victim, killer);
        for (ItemStack stack : items) {

            if (Gear.has(stack)) {
                GearRarity rar = Gear.Load(stack)
                    .getRarity();
                if (rar.is_unique_item) {
                    SoundUtils.ding(victim.level(), victim.blockPosition());
                }
            }

            if (victim.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                victim.spawnAtLocation(sl, stack, 1F);
            }
        }
    }

}
