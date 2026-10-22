package com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.bases;

import com.robertx22.mine_and_slash.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.mine_and_slash.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import com.robertx22.forge_compat.common.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class BaseBaublesItem extends Item implements IAutoLocName, IAutoModel {

    public BaseBaublesItem(Properties settings, String locname) {

        super(settings);
        this.locname = locname;
    }

    @Override
    public final String locNameForLangFile() {
        return locname;
    }

    String locname;

    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Gear_Items;
    }

    @Override
    public String locNameLangFileGUID() {
        return getFormatedForLangFile(VanillaUTIL.REGISTRY.items().getKey(this)
                .toString());
    }

    
    @Override
    public void generateModel(ItemModelManager manager) {
        manager.generated(this);
    }

    @Override
    public String GUID() {
        return "";
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {

        return InteractionResult.SUCCESS;
    }

}
