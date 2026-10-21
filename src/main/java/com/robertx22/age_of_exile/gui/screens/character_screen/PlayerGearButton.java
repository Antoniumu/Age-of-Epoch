package com.robertx22.age_of_exile.gui.screens.character_screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.a_libraries.trinkets.MyTrinketUtils;
import com.robertx22.age_of_exile.a_libraries.trinkets.TrinketSlots;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.gui.ItemSlotButton;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

import net.minecraft.world.level.Level;
public class PlayerGearButton extends ImageButton {

    public static int xSize = 99;
    public static int ySize = 80;

    static Identifier TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/player_gear.png");
    BaseScreen screen;
    Player player;

    public PlayerGearButton(Player player, BaseScreen screen, int xPos, int yPos) {
        super(xPos, yPos, xSize, ySize, 0, 0, ySize + 1, TEX, (button) -> {
        });
        this.player = player;
        this.screen = screen;

        addItemButton(MyTrinketUtils.get(TrinketSlots.NECKLACE.name, player, 0), 0, 4);
        addItemButton(MyTrinketUtils.get(TrinketSlots.RING.name, player, 0), 0, 22);
        addItemButton(MyTrinketUtils.get(TrinketSlots.RING.name, player, 1), 0, 40);

        addItemButton(player.getItemBySlot(EquipmentSlot.HEAD), 81, 4);
        addItemButton(player.getItemBySlot(EquipmentSlot.CHEST), 81, 22);
        addItemButton(player.getItemBySlot(EquipmentSlot.LEGS), 81, 40);
        addItemButton(player.getItemBySlot(EquipmentSlot.FEET), 81, 58);

        // addItemButton(player.getEquippedStack(EquipmentSlot.MAINHAND), 58, 69);
        //addItemButton(player.getEquippedStack(EquipmentSlot.OFFHAND), 179, 69);

    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float ticks) {
        super.renderButton(matrix, x, y, ticks);

        String str = "Ур.: " + Load.Unit(player)
            .getLevel();

        Minecraft mc = Minecraft.getInstance();

        // player 3d view
        com.robertx22.library_of_exile.utils.RenderUtils.renderEntityInInventory(
            this.x + 25,
            this.y + 18,
            this.x + 74,
            this.y + 76,
            26,
            0.0625F,
            (float) x,
            (float) y,
            mc.player
        );

        com.robertx22.forge_compat.client.AbstractGui.drawString(mc.font, matrix, str, this.x + xSize / 2 - mc.font.width(str) / 2, this.y + 5, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.YELLOW));

        str = "Ср. ур.: " + Load.Unit(mc.player)
            .getAverageILVL();

        GuiUtils.renderScaledText(matrix, this.x + xSize / 2, this.y + 17, 0.8F, str, ChatFormatting.GREEN);

    }

    private void addItemButton(ItemStack stack, int x, int y) {
        screen.publicAddButton(new ItemSlotButton(stack, this.x + x, this.y + y));
    }

}
