package mezz.jei.api.gui.drawable;
import com.robertx22.forge_compat.client.GuiGraphics;
public interface IDrawable {
    int getWidth();
    int getHeight();
    default void draw(GuiGraphics guiGraphics) { draw(guiGraphics, 0, 0); }
    void draw(GuiGraphics guiGraphics, int xOffset, int yOffset);
}
