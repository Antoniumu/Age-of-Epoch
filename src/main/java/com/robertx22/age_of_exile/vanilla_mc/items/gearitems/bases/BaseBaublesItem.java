package com.robertx22.age_of_exile.vanilla_mc.items.gearitems.bases;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Gear_Items;
    }

    @Override
    public String locNameLangFileGUID() {
        return getFormatedForLangFile(BuiltInRegistries.ITEM.getKey(this)
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
