package com.robertx22.mine_and_slash.database.data.profession.items;

import com.robertx22.mine_and_slash.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.mine_and_slash.aoe_data.datapacks.models.ModelHelper;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.SlotFamily;
import com.robertx22.mine_and_slash.database.data.profession.ICreativeTabTiered;
import com.robertx22.mine_and_slash.database.data.profession.LeveledItem;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.NameBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.dropblocks.ProfessionDropSourceBlock;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.TooltipContext;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.IRarityItem;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.StringUTIL;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.AutoItem;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;

public class CraftedSoulItem extends AutoItem implements ICreativeTabTiered, IRarityItem {

    SlotFamily fam;
    String rar;

    public CraftedSoulItem(SlotFamily fam, String rar) {
        super(new Properties());
        this.fam = fam;
        this.rar = rar;
    }

    @Override
    public void generateModel(ItemModelManager manager) {
        new ModelHelper(this, ModelHelper.Type.GENERATED, SlashRef.id("item/stat_soul/family/" + fam.id).toString()).generate();
    }

    public StatSoulData getSoul(ItemStack stack) {
        StatSoulData data = StatSoulData.ofFamily(ExileDB.GearRarities().get(rar), LeveledItem.getTier(stack), fam);

        String force = com.robertx22.forge_compat.common.NbtStackCompat.getOrCreateTag(stack).getStringOr("force_tag", "");
        if (!force.isEmpty()) {
            data.force_tag = force;
        }
        return data;
    }

    
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> list, TooltipFlag pIsAdvanced) {
        try {
            var soul = getSoul(pStack);

            if (soul != null) {
                list.clear();
                if (net.minecraft.client.Minecraft.getInstance().hasShiftDown() && soul.gear != null) {
                    soul.gear.gear.BuildTooltip(new com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.TooltipContext(pStack, list, Load.Unit(ClientOnly.getPlayer())));
                } else {
                    ExileTooltips tooltip = soul.getTooltip(pStack, false);
                    tooltip.accept(new NameBlock(Collections.singletonList(pStack.getHoverName())));
                    tooltip.accept(new ProfessionDropSourceBlock(Professions.GEAR_CRAFTING));
                    list.addAll(tooltip.release());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Item getThis() {
        return this;
    }

    @Override
    public String locNameForLangFile() {
        return "Crafted " + StringUTIL.capitalise(rar) + " " + StringUTIL.capitalise(fam.id) + " Soul";
    }

    @Override
    public String GUID() {
        return null;
    }

    @Override
    public GearRarity getItemRarity(ItemStack stack) {
        return ExileDB.GearRarities().get(rar);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }

}
