package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.IdKey;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.misc.ShapedRecipeUTIL;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.world.item.Item;

public class DungeonOrbs extends ExileKeyHolder<ExileCurrency> {

    public static DungeonOrbs INSTANCE = (DungeonOrbs) new DungeonOrbs(DungeonMain.REGISTER_INFO)
            .itemIds(new ItemIdProvider(x -> DungeonMain.id(x)))
            .createItems(new ItemCreator<ExileCurrency>(x -> new Item(new Item.Properties().stacksTo(64))), x -> RegObj.register(x.itemID(), x.item(), DungeonEntries.ITEMS));

    private DungeonOrbs(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<ExileCurrency, IdKey> UBER_UPGRADE = ExileCurrency.Builder.of("uber_upgrade", "Dungeon Map Uber Upgrade", DungeonItemReqs.INSTANCE.IS_MAP_ITEM)
            .addAlwaysUseModification(DungeonItemMods.INSTANCE.UBER_UPGRADE)
            .weight(0)
            .potentialCost(0)
            .build(this);

    @Override
    public void loadClass() {

        UBER_UPGRADE.addRecipe(LibDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 1)
                    .define('Y', DungeonEntries.UBER_FRAGMENT.get())
                    .pattern("YY")
                    .pattern("YY");
        });

    }
}
