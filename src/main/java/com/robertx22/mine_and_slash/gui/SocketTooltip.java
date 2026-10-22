package com.robertx22.mine_and_slash.gui;

import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.SocketData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SocketTooltip implements ClientTooltipComponent {
    private final List<SocketData> gems;
    private final ItemStack stack;

    public SocketTooltip(SocketComponent comp) {
        this.gems = comp.gems();
        this.stack = comp.stack();
    }

    @Override
    public int getHeight(Font font) {
        return 19;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(20, gems.size() * 18);
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor gfx) {
        int xp = x;
        for (SocketData socket : gems) {
            ItemStack gemStack = socket.getInsertedItemStack();
            if (gemStack != null && !gemStack.isEmpty()) {
                gfx.fakeItem(gemStack, xp, y);
            }
            xp += 18;
        }
    }

    public record SocketComponent(ItemStack stack, List<SocketData> gems) implements TooltipComponent {
    }
}
