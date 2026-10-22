package com.robertx22.orbs_of_crafting.register;

import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.mods.DestroyItemMod;
import com.robertx22.orbs_of_crafting.register.mods.DoNothingItemMod;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.reqs.vanilla.VanillaItemMod;

public class Modifications extends ExileKeyHolder<ItemModification> {
    public Modifications(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public static Modifications INSTANCE = new Modifications(Ref.REGISTER_INFO);

    public ExileKey<ItemModification, KeyInfo> DESTROY_ITEM = ExileKey.ofId(this, "destroy_item", x -> new DestroyItemMod(x.GUID()));
    public ExileKey<ItemModification, KeyInfo> DO_NOTHING = ExileKey.ofId(this, "do_nothing", x -> new DoNothingItemMod(x.GUID()));
    public ExileKey<ItemModification, KeyInfo> ENCHANT_30_LEVELS = ExileKey.ofId(this, "enchant_with_30_levels", x -> new VanillaItemMod(x.GUID(), "Applies Enchantments worth 30 Levels", 30));
    public ExileKey<ItemModification, KeyInfo> ENCHANT_20_LEVELS = ExileKey.ofId(this, "enchant_with_20_levels", x -> new VanillaItemMod(x.GUID(), "Applies Enchantments worth 20 Levels", 20));
    public ExileKey<ItemModification, KeyInfo> ENCHANT_10_LEVELS = ExileKey.ofId(this, "enchant_with_10_levels", x -> new VanillaItemMod(x.GUID(), "Applies Enchantments worth 10 Levels", 10));

    @Override
    public void loadClass() {
    }
}
