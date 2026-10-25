package com.robertx22.age_of_exile.compat.jei.category;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared two layouts for mechanics that all have the same shape:
 *
 * <ul>
 *   <li>a transform: up to three consumed ingredients -> up to four results;</li>
 *   <li>a pool: one source/container on the left -> a 6x3 grid of possible drops.</li>
 * </ul>
 *
 * Each slot can cycle through a list of stacks, which is what makes compact pages such
 * as "one page per gear slot" possible while every rarity/tier/base remains findable.
 */
public class DisplayCategory extends AbstractRecipeCategory<DisplayRecipe> {

    public static final IRecipeType<DisplayRecipe> STAT_SOUL =
        IRecipeType.create(SlashRef.MODID, "stat_soul", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> SOUL_IDENTIFY =
        IRecipeType.create(SlashRef.MODID, "soul_identify", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> GEM_SOCKETING =
        IRecipeType.create(SlashRef.MODID, "gem_socketing", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> RARITY_UPGRADE =
        IRecipeType.create(SlashRef.MODID, "rarity_upgrade", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> REPAIR =
        IRecipeType.create(SlashRef.MODID, "repair", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> BACKPACK_UPGRADE =
        IRecipeType.create(SlashRef.MODID, "backpack_upgrade", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> CRATE_OPENING =
        IRecipeType.create(SlashRef.MODID, "crate_opening", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> UNIQUE_GEAR =
        IRecipeType.create(SlashRef.MODID, "unique_gear", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> ITEM_ACTIONS =
        IRecipeType.create(SlashRef.MODID, "item_actions", DisplayRecipe.class);
    public static final IRecipeType<DisplayRecipe> WORLD_SOURCES =
        IRecipeType.create(SlashRef.MODID, "world_sources", DisplayRecipe.class);

    private final int height;
    private final boolean pool;

    public DisplayCategory(
        IGuiHelper guiHelper,
        IRecipeType<DisplayRecipe> type,
        String titleKey,
        ItemStack icon,
        int height,
        boolean pool
    ) {
        super(
            type,
            Component.translatable(titleKey),
            guiHelper.createDrawableItemStack(icon),
            170,
            height
        );
        this.height = height;
        this.pool = pool;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        if (pool) {
            setPool(builder, recipe);
        } else {
            setTransform(builder, recipe);
        }
    }

    private void setTransform(IRecipeLayoutBuilder builder, DisplayRecipe recipe) {
        List<List<ItemStack>> inputs = limited(recipe.inputs(), 3);
        int[] inputX = inputX(inputs.size());

        for (int i = 0; i < inputs.size(); i++) {
            JeiLayout.addSlot(builder, RecipeIngredientRole.INPUT, inputX[i], 1, inputs.get(i));
        }

        List<ItemStack> catalyst = JeiLayout.clean(recipe.catalyst());
        if (!catalyst.isEmpty()) {
            if (inputs.isEmpty()) {
                JeiLayout.addSlot(builder, RecipeIngredientRole.CRAFTING_STATION, 1, 1, catalyst);
            } else if (inputs.size() == 1) {
                JeiLayout.addSlot(builder, RecipeIngredientRole.CRAFTING_STATION, 37, 1, catalyst);
            }
        }

        int outputX = outputX(recipe);
        List<List<ItemStack>> outputs = limited(recipe.outputs(), 4);
        for (int i = 0; i < outputs.size(); i++) {
            JeiLayout.addSlot(builder, RecipeIngredientRole.OUTPUT, outputX + i * 18, 1, outputs.get(i));
        }
    }

    private void setPool(IRecipeLayoutBuilder builder, DisplayRecipe recipe) {
        List<List<ItemStack>> inputs = recipe.inputs();
        if (!inputs.isEmpty()) {
            JeiLayout.addSlot(builder, RecipeIngredientRole.INPUT, 1, 1, inputs.get(0));
        } else if (!JeiLayout.clean(recipe.catalyst()).isEmpty()) {
            JeiLayout.addSlot(builder, RecipeIngredientRole.CRAFTING_STATION, 1, 1, recipe.catalyst());
        }

        List<List<ItemStack>> outputs = limited(recipe.outputs(), 18);
        for (int i = 0; i < outputs.size(); i++) {
            int x = 52 + (i % 6) * 18;
            int y = 1 + (i / 6) * 18;
            JeiLayout.addSlot(builder, RecipeIngredientRole.OUTPUT, x, y, outputs.get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        if (pool) {
            if (!recipe.inputs().isEmpty() || !JeiLayout.clean(recipe.catalyst()).isEmpty()) {
                builder.addRecipeArrowWidget().setPosition(26, 3);
            }
            int outputSlots = Math.min(18, recipe.outputs().size());
            int textY = outputSlots > 12 ? 60 : outputSlots > 6 ? 42 : 26;
            JeiLayout.addLines(builder, recipe.lines(), 1, textY, 168, height - textY - 2);
            return;
        }

        List<List<ItemStack>> inputs = limited(recipe.inputs(), 3);
        boolean hasCatalyst = !JeiLayout.clean(recipe.catalyst()).isEmpty();
        if (inputs.size() >= 2 || (inputs.size() == 1 && hasCatalyst)) {
            builder.addRecipePlusSignWidget().setPosition(26, 3);
        }
        if (!recipe.outputs().isEmpty()) {
            builder.addRecipeArrowWidget().setPosition(outputX(recipe) - 26, 3);
        }
        JeiLayout.addLines(builder, recipe.lines(), 1, 26, 168, height - 28);
    }

    private static int[] inputX(int count) {
        if (count <= 1) {
            return new int[]{1};
        }
        if (count == 2) {
            return new int[]{1, 37};
        }
        return new int[]{1, 24, 47};
    }

    private static int outputX(DisplayRecipe recipe) {
        int inputs = Math.min(3, recipe.inputs().size());
        boolean catalyst = !JeiLayout.clean(recipe.catalyst()).isEmpty();
        if (inputs == 0) {
            return 52;
        }
        if (inputs == 1) {
            return catalyst ? 92 : 52;
        }
        if (inputs == 2) {
            return 92;
        }
        return 98;
    }

    /**
     * JEI pages have a fixed slot budget. Overflow variants are merged into the last
     * cycling slot instead of being lost, so lookups still find them.
     */
    private static List<List<ItemStack>> limited(List<List<ItemStack>> slots, int max) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        if (slots.size() <= max) {
            return slots;
        }
        List<List<ItemStack>> result = new ArrayList<>();
        for (int i = 0; i < max - 1; i++) {
            result.add(slots.get(i));
        }
        List<ItemStack> merged = new ArrayList<>();
        for (int i = max - 1; i < slots.size(); i++) {
            merged.addAll(JeiLayout.clean(slots.get(i)));
        }
        result.add(merged);
        return result;
    }

    @Override
    public Identifier getIdentifier(DisplayRecipe recipe) {
        return recipe.id();
    }
}
