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
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.ingredient.items.IngredientItem;
import com.robertx22.age_of_exile.player_skills.items.alchemy.PotionType;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulItem;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.rarity_gear.RarityGearCrateItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.main.ModItems;
import com.robertx22.forge_compat.common.CreativeModeTab;
import com.robertx22.forge_compat.fml.RegistryObject;
import com.robertx22.forge_compat.registries.ForgeRegistries;
import com.robertx22.infinite_dungeons.item.RewardCrateItem;
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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
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
            sink -> {
                SlashItems.GearItems.STAFFS.values().forEach(r -> acceptRegObj(sink, r));
                SlashItems.GearItems.RINGS.values().forEach(r -> acceptRegObj(sink, r));
                SlashItems.GearItems.NECKLACES.values().forEach(r -> acceptRegObj(sink, r));

                acceptRegObj(sink, SlashItems.TELEPORTER_BLOCK);
                acceptRegObj(sink, SlashItems.RUNEWORD_STATION);
                acceptRegObj(sink, SlashItems.PORTAL);

                acceptRegObj(sink, SlashItems.BACKPACK);
                acceptRegObj(sink, SlashItems.MATERIAL_POUCH);
                BackpackItems.ALL.forEach(r -> acceptRegObj(sink, r));

                acceptRegObj(sink, SlashItems.NEWBIE_GEAR_BAG);
                acceptRegObj(sink, SlashItems.IDENTIFY_TOME);
                acceptRegObj(sink, SlashItems.SALVAGE_HAMMER);
                acceptRegObj(sink, SlashItems.SOCKET_EXTRACTOR);
                acceptRegObj(sink, SlashItems.TELEPORT_BACK);
                acceptRegObj(sink, SlashItems.SOURCE_OF_STRENGTH);
                acceptRegObj(sink, SlashItems.FULL_FAVOR);
                acceptRegObj(sink, SlashItems.EMPTY_FAVOR);

                acceptRegObj(sink, SlashItems.RESET_ALL_PERKS);
                acceptRegObj(sink, SlashItems.ADD_RESET_PERK_POINTS);
                acceptRegObj(sink, SlashItems.RESET_SPELLS);
                acceptRegObj(sink, SlashItems.RESET_STATS);
                acceptRegObj(sink, SlashItems.DEATH_POTION);

                try {
                    for (GearRarity rar : ExileDB.GearRarities().getList()) {
                        acceptStack(sink, RarityGearCrateItem.ofRarity(rar.GUID()));
                    }
                } catch (Exception ignored) {
                }
                acceptRegObj(sink, SlashItems.RARITY_GEAR_CRATE);

                try {
                    NonNullList<ItemStack> crates = NonNullList.create();
                    SlashItems.LOOT_CRATE.get().fillItemCategory(MyModTab, crates);
                    crates.forEach(s -> acceptStack(sink, s));
                } catch (Exception ignored) {
                }
                acceptRegObj(sink, SlashItems.LOOT_CRATE);
                acceptRegObj(sink, SlashItems.LOOT_TABLE_ITEM);
            }
        );

        // 2. Эпоха Изгнания: Души снаряжения
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "gear_souls"),
            "itemGroup.mmorpg_gear_soul_group",
            () -> safeStack(() -> GearSouls.makeIcon()),
            sink -> {
                acceptRegObj(sink, SlashItems.COMMON_SOUL_PRODUCE);

                try {
                    for (GearRarity rar : ExileDB.GearRarities().getList()) {
                        acceptStack(sink, StatSoulItem.ofAnySlotOfRarity(rar.GUID()));
                    }
                } catch (Exception ignored) {
                }

                try {
                    NonNullList<ItemStack> souls = NonNullList.create();
                    SlashItems.STAT_SOUL.get().fillItemCategory(GearSouls, souls);
                    souls.forEach(s -> acceptStack(sink, s));
                } catch (Exception ignored) {
                }
                acceptRegObj(sink, SlashItems.STAT_SOUL);
            }
        );

        // 3. Эпоха Изгнания: Валюта и сферы
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "currency"),
            "itemGroup.mmorpg_currency",
            () -> safeStack(() -> GemRuneCurrency.makeIcon()),
            sink -> {
                CurrencyItems.getAllCurrenciesFromRegistry().forEach(item -> acceptItem(sink, item));

                try {
                    NonNullList<ItemStack> essences = NonNullList.create();
                    SlashItems.RARITY_ESSENCE.get().fillItemCategory(GemRuneCurrency, essences);
                    essences.forEach(s -> acceptStack(sink, s));
                } catch (Exception ignored) {
                }
                acceptRegObj(sink, SlashItems.RARITY_ESSENCE);

                try {
                    NonNullList<ItemStack> upgrades = NonNullList.create();
                    SlashItems.RARITY_UPGRADE.get().fillItemCategory(GemRuneCurrency, upgrades);
                    upgrades.forEach(s -> acceptStack(sink, s));
                } catch (Exception ignored) {
                }
                acceptRegObj(sink, SlashItems.RARITY_UPGRADE);
            }
        );

        // 4. Эпоха Изгнания: Самоцветы и руны
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "gems_and_runes"),
            "itemGroup.mmorpg_gem",
            () -> safeStack(() -> Gems.makeIcon()),
            sink -> {
                GemItems.ALL.forEach(r -> acceptRegObj(sink, r));
                RuneItems.ALL.forEach(r -> acceptRegObj(sink, r));
            }
        );

        // 5. Эпоха Изгнания: Материалы и ингредиенты
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "materials"),
            "itemGroup.mmorpg_mats",
            () -> safeStack(() -> Materials.makeIcon()),
            sink -> {
                acceptRegObj(sink, SlashItems.INFUSED_IRON);
                acceptRegObj(sink, SlashItems.CRYSTALLIZED_ESSENCE);
                acceptRegObj(sink, SlashItems.GOLDEN_ORB);
                acceptRegObj(sink, SlashItems.MYTHIC_ESSENCE);

                for (SkillItemTier tier : SkillItemTier.values()) {
                    acceptRegObj(sink, ProfessionItems.SMELTED_ESSENCE.get(tier));
                }
                for (SkillItemTier tier : SkillItemTier.values()) {
                    acceptRegObj(sink, ProfessionItems.CONDENSED_ESSENCE_MAP.get(tier));
                }
                for (SkillItemTier tier : SkillItemTier.values()) {
                    acceptRegObj(sink, ProfessionItems.SALVAGED_ESSENCE_MAP.get(tier));
                }
                for (SkillItemTier tier : SkillItemTier.values()) {
                    acceptRegObj(sink, ProfessionItems.STONE_TIER_MAP.get(tier));
                }

                NonNullList<ItemStack> tieredIngredients = NonNullList.create();
                try {
                    if (IngredientItems.BLACKPEARL != null && IngredientItems.BLACKPEARL.get() != null) {
                        IngredientItems.BLACKPEARL.get().fillItemCategory(Materials, tieredIngredients);
                    }
                } catch (Exception ignored) {
                }
                if (!tieredIngredients.isEmpty()) {
                    tieredIngredients.forEach(s -> acceptStack(sink, s));
                } else {
                    ForgeRegistries.ITEMS.stream()
                        .filter(x -> x instanceof IngredientItem)
                        .forEach(item -> acceptItem(sink, item));
                }
            }
        );

        // 6. Эпоха Изгнания: Профессии и алхимия
        registerTab(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "professions"),
            "itemGroup.mmorpg_proffs_group",
            () -> safeStack(() -> Professions.makeIcon()),
            sink -> {
                acceptRegObj(sink, CraftToolItems.COOKING);
                acceptRegObj(sink, CraftToolItems.ALCHEMY);
                acceptRegObj(sink, CraftToolItems.INSCRIBING);
                acceptRegObj(sink, CraftToolItems.ARMOR);
                acceptRegObj(sink, CraftToolItems.WEAPON);
                acceptRegObj(sink, CraftToolItems.JEWEL);
                acceptRegObj(sink, SlashItems.ESSENCE_PAPER);

                for (SkillItemTier tier : SkillItemTier.values()) {
                    for (PotionType type : PotionType.values()) {
                        acceptRegObj(sink, AlchemyPotions.POTIONS_MAP.get(ImmutablePair.of(tier, type)));
                    }
                }

                CraftedConsumableItems.FOODS.forEach(r -> acceptRegObj(sink, r));
                CraftedConsumableItems.POTIONS.forEach(r -> acceptRegObj(sink, r));
                CraftedConsumableItems.SCROLLS.forEach(r -> acceptRegObj(sink, r));
            }
        );

        // 7. Божественные миссии
        registerTab(
            Identifier.fromNamespaceAndPath(DivineMissions.MODID, "creative_tab"),
            "itemGroup.divine_missions.creative_tab",
            () -> safeStack(() -> DivineMissions.CreativeTab.makeIcon()),
            sink -> {
                acceptRegistryObj(sink, ModItems.MISSION_ITEM);
                acceptRegistryObj(sink, ModItems.SHRINE);
            }
        );

        // 8. Бесконечные подземелья
        registerTab(
            Identifier.fromNamespaceAndPath(MainID.MODID, "creative_tab"),
            "itemGroup.infinite_dungeons.creative_tab",
            () -> safeStack(() -> MainID.ITEM_GROUP.makeIcon()),
            sink -> {
                acceptRegistryObj(sink, DungeonItems.SKELETAL_KEY);
                acceptRegistryObj(sink, DungeonItems.NETHER_KEY);
                acceptRegistryObj(sink, DungeonItems.VOID_KEY);
                acceptRegistryObj(sink, DungeonItems.SKELETAL_COIN);
                acceptRegistryObj(sink, DungeonItems.NETHER_COIN);
                acceptRegistryObj(sink, DungeonItems.VOID_COIN);

                try {
                    NonNullList<ItemStack> rewardCrates = NonNullList.create();
                    Item crateItem = DungeonItems.REWARD_CRATE.get();
                    if (crateItem instanceof RewardCrateItem rewardCrateItem) {
                        rewardCrateItem.fillItemCategory(MainID.ITEM_GROUP, rewardCrates);
                    }
                    rewardCrates.forEach(s -> acceptStack(sink, s));
                } catch (Exception ignored) {
                }
                acceptRegistryObj(sink, DungeonItems.REWARD_CRATE);
            }
        );
    }

    private static void registerTab(
        Identifier id,
        String translationKey,
        Supplier<ItemStack> iconSupplier,
        Consumer<Consumer<ItemStack>> populator
    ) {
        net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator displayGenerator =
            (net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator) Proxy.newProxyInstance(
                net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator.class.getClassLoader(),
                new Class<?>[]{net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator.class},
                (proxy, method, args) -> {
                    if (args != null && args.length == 2 && args[1] != null) {
                        Object output = args[1];
                        Method acceptMethod = null;
                        for (Method m : output.getClass().getMethods()) {
                            if (m.getName().equals("accept") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == ItemStack.class) {
                                acceptMethod = m;
                                acceptMethod.setAccessible(true);
                                break;
                            }
                        }
                        final Method resolvedAccept = acceptMethod;
                        if (resolvedAccept != null) {
                            Consumer<ItemStack> sink = stack -> {
                                try {
                                    resolvedAccept.invoke(output, stack);
                                } catch (Exception ignored) {
                                }
                            };
                            populator.accept(sink);
                        }
                    }
                    return null;
                }
            );

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

    private static void acceptRegObj(Consumer<ItemStack> sink, RegObj<? extends Item> regObj) {
        if (regObj == null) return;
        try {
            acceptItem(sink, regObj.get());
        } catch (Exception ignored) {
        }
    }

    private static void acceptRegistryObj(Consumer<ItemStack> sink, RegistryObject<? extends Item> regObj) {
        if (regObj == null) return;
        try {
            acceptItem(sink, regObj.get());
        } catch (Exception ignored) {
        }
    }

    private static void acceptItem(Consumer<ItemStack> sink, Item item) {
        if (item == null || item == Items.AIR) return;
        acceptStack(sink, new ItemStack(item));
    }

    private static void acceptStack(Consumer<ItemStack> sink, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        try {
            if (stack.getCount() != 1) {
                stack = stack.copyWithCount(1);
            }
            sink.accept(stack);
        } catch (Exception ignored) {
        }
    }
}
