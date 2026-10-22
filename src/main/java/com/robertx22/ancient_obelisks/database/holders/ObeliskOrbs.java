package com.robertx22.ancient_obelisks.database.holders;

import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.IdKey;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.misc.ShapedRecipeUTIL;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ObeliskOrbs extends ExileKeyHolder<ExileCurrency> {

    public static ObeliskOrbs INSTANCE = (ObeliskOrbs) new ObeliskOrbs(ObelisksMain.REGISTER_INFO)
            .itemIds(new ItemIdProvider(x -> ObelisksMain.id("orbs/" + x)))
            .createItems(new ItemCreator<ExileCurrency>(x -> new Item(new Item.Properties().stacksTo(64))), x -> RegObj.register(x.itemID(), x.item(), ObeliskEntries.ITEMS));

    public ObeliskOrbs(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<ExileCurrency, IdKey> ADD_WAVE = ExileCurrency.Builder.of("add_wave", "Orb of Obelisk Waves", ObeliskItemReqs.INSTANCE.IS_OBELISK_MAP)
            .addRequirement(ObeliskItemReqs.INSTANCE.HAS_LESS_THAN_10_WAVES)
            .addAlwaysUseModification(ObeliskItemMods.INSTANCE.ADD_WAVE)
            .potentialCost(0)
            .weight(0)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> ADD_TIER = ExileCurrency.Builder.of("add_tier", "Orb of Obelisk Difficulty", ObeliskItemReqs.INSTANCE.IS_OBELISK_MAP)
            .addRequirement(ObeliskItemReqs.INSTANCE.IS_LESS_THAN_MAX_TIER)
            .addAlwaysUseModification(ObeliskItemMods.INSTANCE.ADD_TIER)
            .potentialCost(0)
            .weight(0)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> ADD_SPAWN_RATE = ExileCurrency.Builder.of("add_spawn_rate", "Orb of Obelisk Spawns", ObeliskItemReqs.INSTANCE.IS_OBELISK_MAP)
            .addRequirement(ObeliskItemReqs.INSTANCE.SPAWN_RATE_CAP)
            .addAlwaysUseModification(ObeliskItemMods.INSTANCE.ADD_SPAWN_RATE)
            .potentialCost(0)
            .weight(0)
            .build(this);

    @Override
    public void loadClass() {

        // todo make custom items: wrath/envy etc of the ancients

        ADD_WAVE.addRecipe(LibDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 8)
                    .define('X', Items.IRON_INGOT)
                    .define('Y', ObeliskEntries.WRATH.get())
                    .pattern("YYY")
                    .pattern("YXY")
                    .pattern("YYY");
        });

        ADD_TIER.addRecipe(LibDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 8)
                    .define('X', Items.IRON_INGOT)
                    .define('Y', ObeliskEntries.ENVY.get())
                    .pattern("YYY")
                    .pattern("YXY")
                    .pattern("YYY");
        });

        ADD_SPAWN_RATE.addRecipe(LibDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 8)
                    .define('X', Items.DIAMOND)
                    .define('Y', ObeliskEntries.GREED.get())
                    .pattern("YYY")
                    .pattern("YXY")
                    .pattern("YYY");
        });
    }
}
