package com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.bases;

import com.robertx22.forge_compat.common.Tiers;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import net.minecraft.world.item.Item;

public abstract class SingleTargetWeapon extends Item implements IAutoLocName {
    String locname;
    public float attackSpeed = -2.4F;

    public SingleTargetWeapon(Tiers mat, Properties settings, String locname) {
        super(settings);
        this.locname = locname;
    }

    @Override
    public final String locNameForLangFile() {
        return locname;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Gear_Items;
    }

    @Override
    public String locNameLangFileGUID() {
        return VanillaUTIL.REGISTRY.items().getKey(this).toString();
    }

    @Override
    public String GUID() {
        return "";
    }
}
