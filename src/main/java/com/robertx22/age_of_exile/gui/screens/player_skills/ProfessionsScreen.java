package com.robertx22.age_of_exile.gui.screens.player_skills;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.database.data.player_skills.PlayerSkill;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.library_of_exile.utils.CLOC;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

import java.util.Comparator;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
public class ProfessionsScreen extends BaseScreen implements INamedScreen {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/skills/skills_background.png");

    static int sizeX = 256;
    static int sizeY = 207;

    Minecraft mc = Minecraft.getInstance();

    public ProfessionsScreen() {
        super(sizeX, sizeY);
    }

    @Override
    public Identifier iconLocation() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/skills.png");
    }

    @Override
    public Words screenName() {
        return Words.Skills;
    }

    PlayerSkill currentSkill;

    public void setCurrentSkill(PlayerSkill skill) {
        this.currentSkill = skill;

        this.init();

    }

    @Override
    public void init() {
        super.init();

        this.buttons.clear();

        int num = 0;

        int x = guiLeft + 10;
        int y = guiTop + 8;

        List<PlayerSkill> all = ExileDB.PlayerSkills()
            .getList();
        all.sort(Comparator.comparingInt(s -> s.order));

        int fitamount = this.width / (ProfessionButton.BUTTON_SIZE_X + 4);

        if (fitamount > all.size()) {
            fitamount = all.size();
        }
        int spaceLeft = sizeX - ((ProfessionButton.BUTTON_SIZE_X + 4) * fitamount);

        x += spaceLeft / 2;

        for (PlayerSkill skill : all) {

            if (currentSkill == null) {
                currentSkill = skill;
            }

            addButton(new ProfessionButton(this, skill,
                (int) (x + (num * (ProfessionButton.BUTTON_SIZE_X + 4))), y));
            num++;
        }

        if (currentSkill.type_enum.wiki != null) {
            //   addButton(new ExtraInfoButton(Arrays.asList(Component.literal("Открыть вики")), guiLeft + 20, guiTop + sizeY - 50, a -> {
            //      WikiScreen wiki = new WikiScreen();
            //     wiki.changeType(currentSkill.type_enum.wiki);
            //    mc.setScreen(wiki);
            //}));

        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static int BAR_X = 227;
    static int BAR_Y = 7;
    static Identifier BAR_TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/skills/bar.png");

    @Override
    public void render(PoseStack matrix, int x, int y, float ticks) {

        com.robertx22.forge_compat.client.AbstractGui.bindTexture(BACKGROUND);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, mc.getWindow()
                .getGuiScaledWidth() / 2 - sizeX / 2,
            mc.getWindow()
                .getGuiScaledHeight() / 2 - sizeY / 2, 0, 0, sizeX, sizeY
        );

        super.render(matrix, x, y, ticks);

        String nametext = CLOC.translate(Words.Professions.locName());

        //TextUtils.renderText(matrix, 1.5F, nametext, guiLeft + sizeX / 2, guiTop - 15, Formatting.GREEN);

        if (currentSkill != null) {

            String name = currentSkill.type_enum.word.translate() + " " + "(Level " + Load.playerRPGData(mc.player).professions
                .getProfessionLevel(currentSkill.type_enum) + ")";
            renderTextAtMiddle(matrix, name, guiTop + 50, ChatFormatting.GOLD);
            // renderTextAtMiddle(matrix,, guiTop + 65, Formatting.GREEN);

            List<FormattedCharSequence> list = mc.font.split(currentSkill.type_enum.desc.locName(), sizeX - 25);

            int ypos = guiTop + 65;

            for (FormattedCharSequence txt : list) {

                this.renderTextAtMiddle(matrix, txt, ypos, ChatFormatting.WHITE);
                ypos += mc.font.lineHeight + 2;
            }

            renderBar(matrix);

        }

        buttons.forEach(b -> b.renderToolTip(matrix, x, y));

    }

    void renderBar(PoseStack matrix) {
        float filledMulti = Load.playerRPGData(mc.player).professions
            .getExpDividedByNeededToLevelMulti(currentSkill.type_enum);

        int y = guiTop + sizeY - 20;

        com.robertx22.forge_compat.client.AbstractGui.bindTexture(BAR_TEX);
        blit(matrix, mc.getWindow()
                .getGuiScaledWidth() / 2 - BAR_X / 2,
            y, 0, 0, BAR_X, BAR_Y
        );

        blit(matrix, mc.getWindow()
                .getGuiScaledWidth() / 2 - BAR_X / 2,
            y, 0, BAR_Y, (int) (BAR_X * filledMulti), BAR_Y
        );

        PlayerSkillData data = Load.playerRPGData(mc.player).professions
            .getDataFor(currentSkill.type_enum);
        int exp = data.getExp();
        int needed = data.getExpNeededToLevel();
        String xptext = exp + "/" + needed;

        renderTextAtMiddle(matrix, xptext, y - 10, ChatFormatting.YELLOW);

    }

    private void renderTextAtMiddle(PoseStack matrix, String text, int y, ChatFormatting format) {
        com.robertx22.forge_compat.client.AbstractGui.drawShadow(mc.font, matrix, text,
            guiLeft + sizeX / 2 - mc.font.width(text) / 2, y
            , com.robertx22.forge_compat.common.CompatChatFormatting.getColor(format));
    }

    private void renderTextAtMiddle(PoseStack matrix, FormattedCharSequence text, int y, ChatFormatting format) {
        com.robertx22.forge_compat.client.AbstractGui.drawShadow(mc.font, matrix, text,
            guiLeft + sizeX / 2 - mc.font.width(text) / 2, y
            , com.robertx22.forge_compat.common.CompatChatFormatting.getColor(format));
    }

    public enum IconRenderType {
        SCREEN, OVERLAY;
    }

}
