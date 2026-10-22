package com.robertx22.age_of_exile.vanilla_mc.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.BaseModificationStation;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.packets.RequestTilePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.robertx22.forge_compat.client.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.MutableComponent;

public abstract class ModificationGui<T extends BaseTileContainer, Tile extends BaseModificationStation> extends AbstractContainerScreen<T> {

    public Tile tile;
    public Minecraft mc;

    public int backgroundCanvasSize = 256;
    public Font font = Minecraft.getInstance().font;
    Identifier background;

    private final Class<Tile> token;

    public ModificationGui(Identifier background, T cont, Inventory inv, MutableComponent text, Class<Tile> token) {
        super(cont, inv, text);
        this.background = background;
        this.mc = Minecraft.getInstance();
        this.token = token;
        resolveTile();
    }

    @SuppressWarnings("unchecked")
    protected void resolveTile() {
        if (this.tile == null && this.menu != null && this.menu.pos != null && this.mc != null && this.mc.level != null) {
            BlockEntity en = this.mc.level.getBlockEntity(this.menu.pos);
            if (en != null && token.isAssignableFrom(en.getClass())) {
                this.tile = (Tile) en;
            }
        }
    }

    public int putInTheMiddleX(int buttnowidth) {

        int middle = this.leftPos + (this.imageWidth / 2);

        return middle - (buttnowidth / 2);

    }

    static Identifier COOK_ARROW = SlashRef.id("textures/gui/cook_arrow.png");
    static int COOK_X = 24;
    static int COOK_Y = 17;

    public void renderCookArrow(PoseStack matrix, int x, int y, float progress) {
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(COOK_ARROW);
        blit(matrix, x, y, 0, 0, (int) (COOK_X * progress), COOK_Y);
    }

    static Identifier COOK_FLAME = SlashRef.id("textures/gui/cook_flame.png");
    static int FLAME_X = 14;
    static int FLAME_Y = 14;

    public void renderCookFlame(PoseStack matrix, int x, int y, float progress) {
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(COOK_FLAME);

        int offset = (int) (FLAME_Y * (1F - progress));
        blit(matrix, x, y + offset, 0, offset, FLAME_X, (int) (FLAME_Y * progress));

    }

    static Identifier ALCHEMY_ARROW = SlashRef.id("textures/gui/alchemy_arrow.png");
    static int A_ARROW_X = 9;
    static int A_ARROW_Y = 28;

    public void renderAlchemyArrow(PoseStack matrix, int x, int y, float progress) {
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(ALCHEMY_ARROW);

        blit(matrix, x, y, 0, 0, A_ARROW_X, (int) (A_ARROW_Y * progress));

    }

    @Override
    protected void renderLabels(PoseStack matrices, int mouseX, int mouseY) {
        // dont draw "inventory" text lol
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        resolveTile();

        if (tile != null && mc.player != null) {
            if (mc.player.tickCount % 3 == 0) {
                Packets.sendToServer(new RequestTilePacket(tile.getBlockPos()));
            }
        } else if (this.menu != null && this.menu.pos != null && mc.player != null && mc.player.tickCount % 5 == 0) {
            Packets.sendToServer(new RequestTilePacket(this.menu.pos));
        }

        this.renderBackground(matrix);
        super.render(matrix, mouseX, mouseY, partialTicks);
        renderOther(matrix, mouseX, mouseY);
        this.renderTooltip(matrix, mouseX, mouseY);
        this.buttons.forEach(b -> b.renderToolTip(matrix, mouseX, mouseY));

    }

    public void renderOther(PoseStack matrix, int mouseX, int mouseY) {

    }

    @Override
    protected void renderBg(PoseStack matrix, float partialTicks, int x, int y) {

        // Bind the image texture
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(background);
        // Draw the image
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        blit(matrix, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight, backgroundCanvasSize, backgroundCanvasSize);

    }

}

