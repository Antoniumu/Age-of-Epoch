package com.robertx22.age_of_exile.database.base;

import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.AlchemyPotions;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.BackpackItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftToolItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftedConsumableItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CurrencyItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.GemItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.IngredientItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.RuneItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.player_skills.items.alchemy.PotionType;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulItem;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.rarity_gear.RarityGearCrateItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.main.ModItems;
import com.robertx22.forge_compat.common.CreativeModeTab;
import com.robertx22.forge_compat.registries.RegistryObject;
import com.robertx22.infinite_dungeons.main.DungeonItems;
import com.robertx22.infinite_dungeons.main.MainID;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.function.Supplier;

public class CreativeTabs {

    private static boolean registered = false;

    public static CreativeModeTab MyModTab = new CreativeModeTab("mmorpg_main_group") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(SlashItems.GearItems.NECKLACES.get(VanillaMaterial.DIAMOND).get());
        }
    };

    public static CreativeModeTab GearSouls = new CreativeModeTab("mmorpg_gear_soul_group") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.SOUL_LANTERN);
        }
    };

    public static CreativeModeTab Materials = new CreativeModeTab("mmorpg_mats") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(IngredientItems.BOLETE_MUSHROOM.get());
        }
    };

    public static CreativeModeTab GemRuneCurrency = new CreativeModeTab("mmorpg_currency") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(CurrencyItems.ORB_OF_TRANSMUTATION.get());
        }
    };

    public static CreativeModeTab Gems = new CreativeModeTab("mmorpg_gem") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(GemItems.MAP.get(GemItem.GemType.GARNET).get(GemItem.GemRank.GLORIOUS).get());
        }
    };

    public static CreativeModeTab Professions = new CreativeModeTab("mmorpg_proffs_group") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(AlchemyPotions.POTIONS_MAP.get(ImmutablePair.of(SkillItemTier.TIER4, PotionType.HEALTH)).get());
        }
    };

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;

        // 1. Эпоха Изгнания: Основное (оружие, бижутерия, рюкзаки, станции, инструменты, зелья сброса, ящики)
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "main_group"),
            "itemGroup.mmorpg_main_group",
            () -> safeStack(() -> MyModTab.makeIcon()),
            (params, output) -> {
                // Посохи, кольца, ожерелья
                SlashItems.GearItems.STAFFS.values().forEach(r -> acceptItem(output, r));
                SlashItems.GearItems.RINGS.values().forEach(r -> acceptItem(output, r));
                SlashItems.GearItems.NECKLACES.values().forEach(r -> acceptItem(output, r));

                // Станции и блоки
                acceptItem(output, SlashItems.TELEPORTER_BLOCK);
                acceptItem(output, SlashItems.RUNEWORD_STATION);
                acceptItem(output, SlashItems.PORTAL);

                // Рюкзаки и улучшения
                acceptItem(output, SlashItems.BACKPACK);
                acceptItem(output, SlashItems.MATERIAL_POUCH);
                BackpackItems.ALL.forEach(r -> acceptItem(output, r));

                // Инструменты, свитки и расходники
                acceptItem(output, SlashItems.NEWBIE_GEAR_BAG);
                acceptItem(output, SlashItems.IDENTIFY_TOME);
                acceptItem(output, SlashItems.SALVAGE_HAMMER);
                acceptItem(output, SlashItems.SOCKET_EXTRACTOR);
                acceptItem(output, SlashItems.TELEPORT_BACK);
                acceptItem(output, SlashItems.SOURCE_OF_STRENGTH);
                acceptItem(output, SlashItems.FULL_FAVOR);
                acceptItem(output, SlashItems.EMPTY_FAVOR);

                // Зелья сброса
                acceptItem(output, SlashItems.RESET_ALL_PERKS);
                acceptItem(output, SlashItems.ADD_RESET_PERK_POINTS);
                acceptItem(output, SlashItems.RESET_SPELLS);
                acceptItem(output, SlashItems.RESET_STATS);
                acceptItem(output, SlashItems.DEATH_POTION);

                // Ящики снаряжения по редкостям
                try {
                    for (GearRarity rar : ExileDB.GearRarities().getList()) {
                        acceptStack(output, RarityGearCrateItem.ofRarity(rar.GUID()));
                    }
                } catch (Exception ignored) {
                }
                acceptItem(output, SlashItems.RARITY_GEAR_CRATE);

                // Ящики добычи (самоцветы, руны, валюта) по рангам
                try {
                    NonNullList<ItemStack> crates = NonNullList.create();
                    SlashItems.LOOT_CRATE.get().fillItemCategory(MyModTab, crates);
                    crates.forEach(s -> acceptStack(output, s));
                } catch (Exception ignored) {
                }
                acceptItem(output, SlashItems.LOOT_CRATE);
                acceptItem(output, SlashItems.LOOT_TABLE_ITEM);
            }
        );

        // 2. Эпоха Изгнания: Души снаряжения
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "gear_souls"),
            "itemGroup.mmorpg_gear_soul_group",
            () -> safeStack(() -> GearSouls.makeIcon()),
            (params, output) -> {
                acceptItem(output, SlashItems.COMMON_SOUL_PRODUCE);

                // Универсальные души для любого слота по редкостям
                try {
                    for (GearRarity rar : ExileDB.GearRarities().getList()) {
                        acceptStack(output, StatSoulItem.ofAnySlotOfRarity(rar.GUID()));
                    }
                } catch (Exception ignored) {
                }

                // Души по редкостям, слотам и рангам
                try {
                    NonNullList<ItemStack> souls = NonNullList.create();
                    SlashItems.STAT_SOUL.get().fillItemCategory(GearSouls, souls);
                    souls.forEach(s -> acceptStack(output, s));
                } catch (Exception ignored) {
                }
                acceptItem(output, SlashItems.STAT_SOUL);
            }
        );

        // 3. Эпоха Изгнания: Валюта и сферы
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "currency"),
            "itemGroup.mmorpg_currency",
            () -> safeStack(() -> GemRuneCurrency.makeIcon()),
            (params, output) -> {
                CurrencyItems.ITEMS.forEach(item -> acceptItem(output, item));

                // Эссенции редкости
                try {
                    NonNullList<ItemStack> essences = NonNullList.create();
                    SlashItems.RARITY_ESSENCE.get().fillItemCategory(GemRuneCurrency, essences);
                    essences.forEach(s -> acceptStack(output, s));
                } catch (Exception ignored) {
                }
                acceptItem(output, SlashItems.RARITY_ESSENCE);

                // Камни улучшения редкости
                try {
                    NonNullList<ItemStack> upgrades = NonNullList.create();
                    SlashItems.RARITY_UPGRADE.get().fillItemCategory(GemRuneCurrency, upgrades);
                    upgrades.forEach(s -> acceptStack(output, s));
                } catch (Exception ignored) {
                }
                acceptItem(output, SlashItems.RARITY_UPGRADE);
            }
        );

        // 4. Эпоха Изгнания: Самоцветы и руны
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "gems_and_runes"),
            "itemGroup.mmorpg_gem",
            () -> safeStack(() -> Gems.makeIcon()),
            (params, output) -> {
                GemItems.ALL.forEach(r -> acceptItem(output, r));
                RuneItems.ALL.forEach(r -> acceptItem(output, r));
            }
        );

        // 5. Эпоха Изгнания: Материалы и ингредиенты
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "materials"),
            "itemGroup.mmorpg_mats",
            () -> safeStack(() -> Materials.makeIcon()),
            (params, output) -> {
                acceptItem(output, SlashItems.INFUSED_IRON);
                acceptItem(output, SlashItems.CRYSTALLIZED_ESSENCE);
                acceptItem(output, SlashItems.GOLDEN_ORB);
                acceptItem(output, SlashItems.MYTHIC_ESSENCE);

                acceptItem(output, ProfessionItems.SMELTED_ESSENCE);
                for (SkillItemTier tier : SkillItemTier.values()) {
                    acceptItem(output, ProfessionItems.CONDENSED_ESSENCE_MAP.get(tier));
                }
                for (SkillItemTier tier : SkillItemTier.values()) {
                    acceptItem(output, ProfessionItems.SALVAGED_ESSENCE_MAP.get(tier));
                }
                for (SkillItemTier tier : SkillItemTier.values()) {
                    acceptItem(output, ProfessionItems.STONE_TIER_MAP.get(tier));
                }

                // Ремесленные ингредиенты по рангам (с NBT для ремесла)
                NonNullList<ItemStack> tieredIngredients = NonNullList.create();
                try {
                    if (IngredientItems.BLACKPEARL != null && IngredientItems.BLACKPEARL.get() != null) {
                        IngredientItems.BLACKPEARL.get().fillItemCategory(Materials, tieredIngredients);
                    }
                } catch (Exception ignored) {
                }
                if (!tieredIngredients.isEmpty()) {
                    tieredIngredients.forEach(s -> acceptStack(output, s));
                } else {
                    IngredientItems.ALL.forEach(r -> acceptItem(output, r));
                }
            }
        );

        // 6. Эпоха Изгнания: Профессии и алхимия
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "professions"),
            "itemGroup.mmorpg_proffs_group",
            () -> safeStack(() -> Professions.makeIcon()),
            (params, output) -> {
                acceptItem(output, CraftToolItems.COOKING);
                acceptItem(output, CraftToolItems.ALCHEMY);
                acceptItem(output, CraftToolItems.INSCRIBING);
                acceptItem(output, CraftToolItems.ARMOR);
                acceptItem(output, CraftToolItems.WEAPON);
                acceptItem(output, CraftToolItems.JEWELRY);
                acceptItem(output, SlashItems.ESSENCE_PAPER);

                for (SkillItemTier tier : SkillItemTier.values()) {
                    for (PotionType type : PotionType.values()) {
                        acceptItem(output, AlchemyPotions.POTIONS_MAP.get(ImmutablePair.of(tier, type)));
                    }
                }

                CraftedConsumableItems.FOODS.values().forEach(r -> acceptItem(output, r));
                CraftedConsumableItems.POTIONS.values().forEach(r -> acceptItem(output, r));
                CraftedConsumableItems.SCROLLS.values().forEach(r -> acceptItem(output, r));
            }
        );

        // 7. Божественные миссии
        registerTab(
            Identifier.fromNamespaceAndPath(DivineMissions.MODID, "creative_tab"),
            "itemGroup.divine_missions.creative_tab",
            () -> safeStack(() -> DivineMissions.CreativeTab.makeIcon()),
            (params, output) -> {
                acceptItem(output, ModItems.MISSION_ITEM);
                acceptItem(output, ModItems.SHRINE);
            }
        );

        // 8. Бесконечные подземелья
        registerTab(
            Identifier.fromNamespaceAndPath(MainID.MODID, "creative_tab"),
            "itemGroup.infinite_dungeons.creative_tab",
            () -> safeStack(() -> MainID.ITEM_GROUP.makeIcon()),
            (params, output) -> {
                acceptItem(output, DungeonItems.SKELETAL_KEY);
                acceptItem(output, DungeonItems.NETHER_KEY);
                acceptItem(output, DungeonItems.VOID_KEY);
                acceptItem(output, DungeonItems.SKELETAL_COIN);
                acceptItem(output, DungeonItems.NETHER_COIN);
                acceptItem(output, DungeonItems.VOID_COIN);

                try {
                    NonNullList<ItemStack> rewardCrates = NonNullList.create();
                    DungeonItems.REWARD_CRATE.get().fillItemCategory(MainID.ITEM_GROUP, rewardCrates);
                    rewardCrates.forEach(s -> acceptStack(output, s));
                } catch (Exception ignored) {
                }
                acceptItem(output, DungeonItems.REWARD_CRATE);
            }
        );
    }

    private static void registerTab(
        Identifier id,
        String translationKey,
        Supplier<ItemStack> iconSupplier,
        net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator displayGenerator
    ) {
        net.minecraft.world.item.CreativeModeTab tab =
            net.minecraft.world.item.CreativeModeTab.builder(net.minecraft.world.item.CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable(translationKey))
                .icon(iconSupplier)
                .displayItems(displayGenerator)
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
    }

    private static ItemStack safeStack(Supplier<ItemStack> supplier) {
        try {
            ItemStack stack = supplier.get();
            if (stack != null && !stack.isEmpty()) {
                return stack;
            }
        } catch (Exception ignored) {
        }
        return new ItemStack(Items.BOOK);
    }

    private static void acceptItem(net.minecraft.world.item.CreativeModeTab.Output output, RegistryObject<? extends Item> regObj) {
        if (regObj == null) return;
        try {
            Item item = regObj.get();
            acceptItem(output, item);
        } catch (Exception ignored) {
        }
    }

    private static void acceptItem(net.minecraft.world.item.CreativeModeTab.Output output, Item item) {
        if (item == null || item == Items.AIR) return;
        acceptStack(output, new ItemStack(item));
    }

    private static void acceptStack(net.minecraft.world.item.CreativeModeTab.Output output, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        try {
            if (stack.getCount() != 1) {
                stack = stack.copyWithCount(1);
            }
            output.accept(stack);
        } catch (Exception ignored) {
        }
    }
}
