package com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.baubles;

import com.robertx22.mine_and_slash.a_libraries.curios.interfaces.INecklace;
import com.robertx22.mine_and_slash.uncommon.IShapedRecipe;
import com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.bases.BaseBaublesItem;
import com.robertx22.forge_compat.data.ShapedRecipeBuilder;


public class ItemNecklace extends BaseBaublesItem implements INecklace, IShapedRecipe {

    VanillaMaterial mat;

    public ItemNecklace(VanillaMaterial mat) {
        super(new Properties().durability(500 + (int) (160 * 3)), "Necklace");
        this.mat = mat;
    }

    @Override
    public ShapedRecipeBuilder getRecipe() {
        return shaped(this)
                .define('X', mat.mat.item)
                .pattern("XXX")
                .pattern("X X")
                .pattern(" X ")
                .unlockedBy("player_level", trigger());
    }
}
