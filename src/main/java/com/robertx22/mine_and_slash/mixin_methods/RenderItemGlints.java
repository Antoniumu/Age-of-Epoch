package com.robertx22.mine_and_slash.mixin_methods;

import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.mine_and_slash.capability.player.container.SkillGemsScreen;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.uncommon.interfaces.IRarityItem;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.ICommonDataItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class RenderItemGlints {

    public static void drawMyGlint(GuiGraphicsExtractor gui, ItemStack pStack, int pX, int pY) {

        try {


            var mc = Minecraft.getInstance();

            if (mc.screen == null) {
                return;
            }

            if (ClientConfigs.getConfig().RENDER_ITEM_RARITY_BACKGROUND.get()) {
                ItemStack stack = pStack;//slot.getItem();

                GearRarity rar = null;

                var data = ICommonDataItem.load(stack);

                if (data != null) {
                    if (data.getRarity() != null) {
                        rar = data.getRarity();
                    }
                }
                if (rar == null) {
                    if (stack.getItem() instanceof IRarityItem ri) {
                        rar = ri.getItemRarity(stack);
                    }
                }
                if (rar == null) {
                    return;
                }

                RenderSystem.enableBlend();
                com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ClientConfigs.getConfig().ITEM_RARITY_OPACITY.get().floatValue()); // transparency

                Identifier tex = rar.getGlintTextureFull();

                if (ClientConfigs.getConfig().ITEM_RARITY_BACKGROUND_TYPE.get() == ClientConfigs.GlintType.BORDER) {
                    tex = rar.getGlintTextureBorder();
                }
                if (Minecraft.getInstance().screen instanceof SkillGemsScreen && data instanceof SkillGemData) {
                    tex = rar.getGlintTextureCircle();
                }

                com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, tex, pX, pY, 0, 0, 16, 16, 16, 16);
                com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1F);
                RenderSystem.disableBlend();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void register() {


    }
}
