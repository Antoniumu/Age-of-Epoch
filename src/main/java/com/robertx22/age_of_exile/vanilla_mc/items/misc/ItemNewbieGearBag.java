package com.robertx22.age_of_exile.vanilla_mc.items.misc;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.loot.generators.GearSoulLootGen;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.forge_compat.fml.ModList;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ItemNewbieGearBag extends Item {

    public ItemNewbieGearBag() {
        super(new Properties());

    }

    static HashMap<String, NewbieContent> MAP = new HashMap<>();
    static NewbieContent defaultContent = new NewbieContent(Arrays.asList(() -> SlashItems.GearItems.STAFFS.get(VanillaMaterial.IRON)
        .get()), Arrays.asList(GearSlots.STAFF));

    static {
    }

    static void giveNewbieItemsFor(Player player, Perk perk) {

        NewbieContent c = defaultContent;

        if (MAP.containsKey(perk.GUID())) {
            c = MAP.get(perk.GUID());
        }

        c.give(player);

    }

    static class NewbieContent {

        public List<String> gearslots;
        public List<Supplier<Item>> items;

        public List<ItemStack> stacks = new ArrayList<>();

        public NewbieContent(List<Supplier<Item>> items, List<String> gearslots) {
            this.gearslots = gearslots;
            this.items = items;

        }

        public NewbieContent addStack(ItemStack stack) {
            this.stacks.add(stack);
            return this;
        }

        public void give(Player player) {

            items.forEach(x -> PlayerUtils.giveItem(new ItemStack(x.get()), player));

            gearslots.forEach(x -> {
                BaseGearType gear = ExileDB.GearTypes()
                    .getFilterWrapped(e -> e.gear_slot.equals(x))
                    .random();
                GearItemData data = getBlueprint(gear).createData();
                data.lvl = 1;
                data.can_sal = false;

                GearBlueprint b = new GearBlueprint(1);
                b.level.set(1);
                b.rarity.set(ExileDB.GearRarities()
                    .get(IRarity.COMMON_ID));
                b.gearItemSlot.set(gear);

                ItemStack stack = GearSoulLootGen.createSoulBasedOnGear(b);

                player.level().registryAccess().lookup(Registries.ENCHANTMENT)
                    .flatMap(reg -> reg.get(Enchantments.UNBREAKING))
                    .ifPresent(holder -> stack.enchant(holder, 3));

                PlayerUtils.giveItem(stack, player);

            });

            stacks.forEach(x -> PlayerUtils.giveItem(x, player));
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

    private static GearBlueprint getBlueprint(BaseGearType type) {
        GearBlueprint print = new GearBlueprint(1);
        print.gearItemSlot.set(type);
        print.rarity.set(ExileDB.GearRarities()
            .get(IRarity.COMMON_ID));
        return print;
    }

}
