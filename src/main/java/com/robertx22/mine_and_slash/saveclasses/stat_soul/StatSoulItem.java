package com.robertx22.mine_and_slash.saveclasses.stat_soul;
import net.minecraft.world.InteractionResult;

import com.robertx22.library_of_exile.registry.IGUID;
import com.robertx22.library_of_exile.utils.LoadSave;
import com.robertx22.mine_and_slash.a_libraries.jei.iHideJei;
import com.robertx22.mine_and_slash.database.data.gear_slots.GearSlot;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.NameBlock;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.TooltipContext;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.ICreativeTabNbt;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import com.robertx22.forge_compat.common.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatSoulItem extends Item implements IGUID, ICreativeTabNbt, iHideJei {

    public static String TAG = "stat_soul";

    public StatSoulItem() {
        super(new Properties());
    }

    public static boolean hasSoul(ItemStack stack) {
        return com.robertx22.forge_compat.common.NbtStackCompat.hasTag(stack) && com.robertx22.forge_compat.common.NbtStackCompat.getTag(stack)
                .contains(TAG);
    }

    public static StatSoulData getSoul(ItemStack stack) {
        StatSoulData data = LoadSave.Load(StatSoulData.class, new StatSoulData(), com.robertx22.forge_compat.common.NbtStackCompat.getOrCreateTag(stack), TAG);
        return data;
    }

    @Override
    public InteractionResult use(Level pLevel, Player p, InteractionHand pUsedHand) {
        ItemStack itemstack = p.getItemInHand(pUsedHand);

        if (!pLevel.isClientSide()) {

            if (StackSaving.STAT_SOULS.has(itemstack)) {

                StatSoulData data = StackSaving.STAT_SOULS.loadFrom(itemstack);

                if (data.gear == null) {
                    var geardata = data.createGearData(null, p);

                    Item item = geardata.get(StackKeys.GEAR).GetBaseGearType().getRandomItem(data.getRarity());

                    ItemStack stack = item.getDefaultInstance();

                    var ex = ExileStack.of(stack);
                    geardata.apply(ex);

                    PlayerUtils.giveItem(ex.getStack(), p);
                    itemstack.shrink(1);
                }
            }
        }

        return InteractionResult.PASS;

    }

    @Override
    public List<ItemStack> createAllVariationsForCreativeTabs() {
        var list = new ArrayList<ItemStack>();
        for (GearRarity rarity : ExileDB.GearRarities()
                .getList()) {
            for (GearSlot slot : ExileDB.GearSlots()
                    .getList()) {
                for (int i = 0; i <= LevelUtils.getMaxTier(); i++) {
                    StatSoulData data = new StatSoulData();
                    data.tier = i;
                    data.rar = rarity.GUID();
                    data.slot = slot.GUID();

                    ItemStack stack = data.toStack();
                    list.add(stack);
                }

            }
        }
        return list;
    }

    @Override
    public Component getName(ItemStack stack) {

        MutableComponent txt = Component.translatable(this.getDescriptionId());

        try {
            StatSoulData data = getSoul(stack);

            if (data == null) {
                return txt;
            } else {

                GearRarity rar = ExileDB.GearRarities()
                        .get(data.rar);
                GearSlot slot = ExileDB.GearSlots()
                        .get(data.slot);

                MutableComponent t = rar.locName();
                if (!data.canBeOnAnySlot()) {
                    t.append(" ")
                            .append(slot.locName());
                }

                t.append(" ")
                        .append(Words.Soul.locName())
                        .withStyle(rar.textFormatting());

                return t;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return txt;
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        try {
            StatSoulData data = StackSaving.STAT_SOULS.loadFrom(stack);
            if (data != null) {
                tooltip.clear();
                if (net.minecraft.client.Minecraft.getInstance().hasShiftDown() && data.gear != null) {
                    // todo temp solution to view extracted souls
                    var gearstack = new ItemStack(Items.IRON_SWORD);
                    data.gear.saveTo(gearstack);
                    data.gear.gear.BuildTooltip(new com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.TooltipContext(gearstack, tooltip, Load.Unit(ClientOnly.getPlayer())));
                } else {
                    ExileTooltips exileTooltips = data.getTooltip(stack, false);
                    exileTooltips.accept(new NameBlock(Collections.singletonList(stack.getHoverName())));

                    tooltip.addAll(exileTooltips.release());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String GUID() {
        return "stat_soul/stat_soul";
    }



    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }

}
