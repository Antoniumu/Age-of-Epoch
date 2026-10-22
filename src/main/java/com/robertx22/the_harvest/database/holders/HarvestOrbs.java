package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import com.robertx22.the_harvest.main.HarvestEntries;
import com.robertx22.the_harvest.main.HarvestMain;
import net.minecraft.world.item.Item;

public class HarvestOrbs extends ExileKeyHolder<ExileCurrency> {

    public static HarvestOrbs INSTANCE = (HarvestOrbs) new HarvestOrbs(HarvestMain.REGISTER_INFO)
            .itemIds(new ItemIdProvider(x -> HarvestMain.id("orbs/" + x)))
            .createItems(new ItemCreator<ExileCurrency>(x -> new Item(new Item.Properties().stacksTo(64))), x -> RegObj.register(x.itemID(), x.item(), HarvestEntries.ITEMS));

    public HarvestOrbs(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    /*
    public ExileKey<ExileCurrency, IdKey> ADD_WAVE = ExileCurrency.Builder.of("add_wave", "Orb of Obelisk Waves", HarvestItemReqs.INSTANCE.IS_OBELISK_MAP)
            .addRequirement(HarvestItemReqs.INSTANCE.HAS_LESS_THAN_10_WAVES)
            .addAlwaysUseModification(HarvestItemMods.INSTANCE.ADD_WAVE)
            .potentialCost(0)
            .weight(0)
            .build(this);


     */

    @Override
    public void loadClass() {

        // todo make custom items: wrath/envy etc of the ancients

        /*
        ADD_WAVE.addRecipe(OrbDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 8)
                    .define('X', Items.IRON_INGOT)
                    .define('Y', ObeliskEntries.WRATH.get())
                    .pattern("YYY")
                    .pattern("YXY")
                    .pattern("YYY");
        });
         */

    }
}
