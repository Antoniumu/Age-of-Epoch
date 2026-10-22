package com.robertx22.mine_and_slash.database.data.profession.items;
import net.minecraft.world.InteractionResult;

import com.robertx22.library_of_exile.tooltip.ExileTooltipUtils;
import com.robertx22.library_of_exile.util.UNICODE;
import com.robertx22.mine_and_slash.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.mine_and_slash.aoe_data.datapacks.models.ModelHelper;
import com.robertx22.mine_and_slash.capability.player.data.PlayerBuffData;
import com.robertx22.mine_and_slash.database.data.profession.ICreativeTabTiered;
import com.robertx22.mine_and_slash.database.data.profession.LeveledItem;
import com.robertx22.mine_and_slash.database.data.profession.buffs.StatBuff;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.OperationTipBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.RequirementBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.affixdatablocks.SimpleItemStatBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.dropblocks.ProfessionDropSourceBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.usableitemblocks.UsageBlock;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.CraftedRarity;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.IRarityItem;
import com.robertx22.mine_and_slash.uncommon.localization.Formatter;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.AutoItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import com.robertx22.forge_compat.common.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CraftedBuffFoodItem extends AutoItem implements IRarityItem, ICreativeTabTiered {

    public String buff_id;
    PlayerBuffData.Type type;
    CraftedRarity rar;

    public CraftedBuffFoodItem(PlayerBuffData.Type type, String buff_id, CraftedRarity rar) {
        super(getProp(type));
        this.buff_id = buff_id;
        this.rar = rar;
        this.type = type;
    }

    static Properties getProp(PlayerBuffData.Type type) {
        if (type.isFood()) {
            return new Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build());
        } else {
            return new Properties();
        }
    }

    @Override
    public void generateModel(ItemModelManager manager) {
        new ModelHelper(this, ModelHelper.Type.GENERATED, SlashRef.id("item/" + this.type.id + "/" + buff_id).toString()).generate();
    }

    @Override
    public InteractionResult use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        return ItemUtils.startUsingInstantly(pLevel, pPlayer, pUsedHand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level pLevel, LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide()) {
            if (pLivingEntity instanceof Player p) {
                boolean did = Load.player(p).buff.tryAdd(p, getBuff(), LeveledItem.getLevel(stack), rar.getPercent(), type, getTicksDuration());
                if (did) {
                    pLivingEntity.addEffect(new MobEffectInstance(com.robertx22.forge_compat.common.CompatHelper.wrapEffect(this.type.effect.get()), getTicksDuration()));
                    stack.shrink(1);
                    return stack;
                }
            }
        }

        return stack;

    }

    public int getTicksDuration() {
        return type.durationTicks;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack pStack) {
        if (type.isFood()) {
            return ItemUseAnimation.EAT;
        } else {
            return ItemUseAnimation.DRINK;
        }
    }

    
    public int getUseDuration(ItemStack pStack) {
        return 32;
    }

    public StatBuff getBuff() {
        return ExileDB.StatBuffs().get(buff_id);
    }

    // Greater intelligence potion = power + name
    @Override
    public Component getName(ItemStack stack) {
        return Formatter.BUFF_CONSUMPTIONS_NAME.locName(this.rar.getRarity().locName(), getBuff().mods.get(0).GetStat().locName(), type.locName())
                .withStyle(rar.getRarity().textFormatting());
    }


    
    public void appendHoverText(ItemStack stack, @Nullable Level pLevel, List<Component> list, TooltipFlag pIsAdvanced) {

        try {

            StatBuff buff = getBuff();
            int lvl = LeveledItem.getLevel(stack);

            List<MutableComponent> info = new ArrayList<>();
            info.add(Component.literal(UNICODE.STAR + " ").append(Itemtips.BUFF_CONSUMABLE_TYPE.locName(this.type.locName().withStyle(ChatFormatting.YELLOW))).withStyle(ChatFormatting.AQUA));
            info.addAll(ExileTooltipUtils.splitLongText(Itemtips.BUFF_CONSUMABLE_INFO.locName().withStyle(ChatFormatting.AQUA)));
            info.add(Component.literal(UNICODE.ROTATED_CUBE + " ")
                    .append(Itemtips.BUFF_CONSUMABLE_DURATION_MINUTES.locName(Component.literal((getTicksDuration() / 20 / 60) + "").withStyle(ChatFormatting.YELLOW))).withStyle(ChatFormatting.GREEN));


            list.addAll(new ExileTooltips()
                    .accept(new SimpleItemStatBlock(new StatRangeInfo(ModRange.always(rar.getPercent())))
                            .accept(Itemtips.BUFF_TIP.locName(), buff.getStats(lvl, rar.getPercent())))
                    .accept(new RequirementBlock(lvl))
                    .accept(new OperationTipBlock().setAlt())
                    .accept(new ProfessionDropSourceBlock(this.type.profession))
                    .accept(new UsageBlock(info))
                    .release());


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Misc;
    }

    //Use Formatter process the item name instead.
    @Override
    public String locNameLangFileGUID() {
        return "";
    }

    @Override
    public String locNameForLangFile() {
        return "";
    }

    @Override
    public String GUID() {
        return "";
    }


    @Override
    public Item getThis() {
        return this;
    }

    @Override
    public GearRarity getItemRarity(ItemStack stack) {
        return rar.getRarity();
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }


    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return this.getUseDuration(stack);
    }

}
