package com.robertx22.age_of_exile.vanilla_mc.items.misc;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.groups.GearRarityGroups;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.stat_soul.SoullessGear;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.forge_compat.fml.ModList;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ItemNewbieGearBag extends Item {

    public ItemNewbieGearBag() {
        super(new Properties());

    }

    static HashMap<String, NewbieContent> MAP = new HashMap<>();

    /**
     * По умолчанию мешок выдаёт готовый посох со случайной душой посоха.
     * Раньше здесь лежал отдельный «голый» посох и отдельно ролл души/предмета —
     * из-за доли дропа готовыми предметами игрок получал два посоха.
     */
    static NewbieContent defaultContent = new NewbieContent(Arrays.asList(GearSlots.STAFF));

    static void giveNewbieItemsFor(Player player, Perk perk) {

        NewbieContent c = defaultContent;

        if (MAP.containsKey(perk.GUID())) {
            c = MAP.get(perk.GUID());
        }

        c.give(player);

    }

    static class NewbieContent {

        public List<String> gearslots;
        public List<ItemStack> stacks = new ArrayList<>();

        public NewbieContent(List<String> gearslots) {
            this.gearslots = gearslots;
        }

        public NewbieContent addStack(ItemStack stack) {
            this.stacks.add(stack);
            return this;
        }

        public void give(Player player) {

            gearslots.forEach(x -> {
                ItemStack stack = createStarterGear(x);
                if (stack.isEmpty()) {
                    return;
                }

                player.level()
                    .registryAccess()
                    .lookup(Registries.ENCHANTMENT)
                    .flatMap(reg -> reg.get(Enchantments.UNBREAKING))
                    .ifPresent(holder -> stack.enchant(holder, 3));

                PlayerUtils.giveItem(stack, player);
            });

            stacks.forEach(x -> PlayerUtils.giveItem(x, player));
        }
    }

    /**
     * Один готовый предмет: бесдушная основа + случайная душа того же слота.
     * Основа берётся железной (как раньше), душа — случайной не униковой редкости.
     */
    public static ItemStack createStarterGear(String slotId) {
        try {
            BaseGearType type = ExileDB.GearTypes()
                .getFilterWrapped(x -> x.gear_slot.equals(slotId))
                .errorIfNothingLeft(false)
                .random();
            if (type == null) {
                return ItemStack.EMPTY;
            }

            GearBlueprint blueprint = new GearBlueprint(1);
            blueprint.gearItemSlot.set(type);

            GearRarity rarity = ExileDB.GearRarities()
                .get(IRarity.COMMON_ID);
            List<GearRarity> possible = ExileDB.GearRarityGroups()
                .get(GearRarityGroups.DROPPABLE_RARITIES_ID)
                .getRarities()
                .stream()
                .filter(x -> x != null && !x.is_unique_item)
                .collect(Collectors.toList());
            if (!possible.isEmpty()) {
                GearRarity rolled = RandomUtils.weightedRandom(possible);
                if (rolled != null) {
                    rarity = rolled;
                }
            }
            if (rarity != null) {
                blueprint.rarity.set(rarity);
            }

            GearItemData soul = blueprint.createData();
            if (soul == null) {
                return ItemStack.EMPTY;
            }
            soul.lvl = 1;

            // Сначала бесдушная основа, потом душа поверх неё — единый путь системы.
            ItemStack stack = SoullessGear.createLootItem(type, 1, 3);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            GearItemData base = Gear.Load(stack);
            if (base != null) {
                SoullessGear.applySoul(soul, base);
            }
            // Стартовый предмет нельзя разобрать на валюту.
            soul.can_sal = false;
            soul.item_id = SoullessGear.itemId(stack);
            Gear.Save(stack, soul);
            return stack;
        } catch (Exception e) {
            e.printStackTrace();
            return ItemStack.EMPTY;
        }
    }

    @Override
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {

        if (!worldIn.isClientSide()) {
            try {

                List<Perk> starts = Load.playerRPGData(playerIn).talents
                    .getAllAllocatedPerks()
                    .values()
                    .stream()
                    .filter(x -> x.is_entry)
                    .collect(Collectors.toList());

                if (true || !starts.isEmpty()) {

                    defaultContent.give(playerIn);

                    if (ModList.get()
                        .isLoaded("patchouli")) {
                    }

                    playerIn.getItemInHand(handIn)
                        .shrink(1);

                } else {
                    playerIn.sendSystemMessage(Component.literal("Выберите свой путь, чтобы открыть это (нажмите [H] и откройте дерево талантов)"));
                }

                return InteractionResult.PASS;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return InteractionResult.PASS;
    }

}
