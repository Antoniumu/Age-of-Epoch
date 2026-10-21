package com.robertx22.age_of_exile.gui.screens.spell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.synergy.Synergy;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.gui.screens.ILeftRight;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.vanilla_mc.packets.AllocateSpellPacket;
import com.robertx22.age_of_exile.vanilla_mc.packets.spells.SetupHotbarPacket;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;

public class SpellScreen extends BaseScreen implements INamedScreen, ILeftRight {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/spell_school_background.png");

    static int sizeX = 250;
    static int sizeY = 233;
    public static boolean IS_PICKING_HOTBAR_SPELL = false;
    public static int NUMBER_OF_HOTBAR_FOR_PICKING = 0;

    public Spell dragCandidateSpell = null;
    public boolean dragFromHotbar = false;
    public boolean isDraggingSpell = false;
    public double dragStartX = 0;
    public double dragStartY = 0;

    private String statusMessage = "";
    private long statusMessageTime = 0L;

    Minecraft mc = Minecraft.getInstance();

    public List<SpellSchool> schoolsInOrder = ExileDB.SpellSchools()
        .getList();
    public int currentIndex = 0;
    public int maxIndex = ExileDB.SpellSchools()
        .getSize() - 1;

    public SpellSchool currentSchool() {
        return schoolsInOrder.get(currentIndex);
    }

    public SpellScreen() {
        super(sizeX, sizeY);
        IS_PICKING_HOTBAR_SPELL = false;
    }

    public void setStatusMessage(String msg) {
        this.statusMessage = msg != null ? msg : "";
        this.statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public Identifier iconLocation() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/spells.png");
    }

    @Override
    public Words screenName() {
        return Words.Skills;
    }

    static int SLOT_SPACING = 21;

    @Override
    public void init() {
        super.init();

        try {
            this.buttons.clear();
            this.children.clear();

            addButton(new LeftRightButton(this, guiLeft + 100 - LeftRightButton.xSize - 5, guiTop + 25 - LeftRightButton.ySize / 2, true));
            addButton(new LeftRightButton(this, guiLeft + 150 + 5, guiTop + 25 - LeftRightButton.ySize / 2, false));

            currentSchool().spells.entrySet()
                .forEach(e -> {

                    PointData point = e.getValue();
                    Spell spell = ExileDB.Spells()
                        .get(e.getKey());

                    if (spell != null) {
                        int x = this.guiLeft + 12 + (point.x * SLOT_SPACING);
                        int y = this.guiTop + 177 - (point.y * SLOT_SPACING);

                        this.addButton(new LearnSpellButton(this, spell, x, y));
                    }
                });

            currentSchool().synergies.entrySet()
                .forEach(e -> {

                    PointData point = e.getValue();
                    Synergy synergy = ExileDB.Synergies()
                        .get(e.getKey());

                    if (synergy != null) {

                        int x = this.guiLeft + 12 + (point.x * SLOT_SPACING);
                        int y = this.guiTop + 177 - (point.y * SLOT_SPACING);

                        this.addButton(new LearnSynergyButton(this, synergy, x, y));
                    }
                });

            for (int i = 0; i < 8; i++) {

                int x = guiLeft + 10 + (PickHotbarButton.BUTTON_SIZE_X * i);

                if (i > 3) {
                    x += 70;
                }
                int y = guiTop + 204;

                this.addButton(new PickHotbarButton(this, i, x, y));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            for (Button b : this.buttons) {
                if (b instanceof LearnSpellButton lsb && lsb.isInside((int) mouseX, (int) mouseY)) {
                    this.dragCandidateSpell = lsb.spell;
                    this.dragFromHotbar = false;
                    this.dragStartX = mouseX;
                    this.dragStartY = mouseY;
                    this.isDraggingSpell = false;
                    lsb.playDownSound(mc.getSoundManager());
                    return true;
                }
                if (b instanceof PickHotbarButton phb && phb.isInside((int) mouseX, (int) mouseY)) {
                    Spell existing = Load.spells(mc.player).getSpellByNumber(phb.num);
                    if (existing != null) {
                        this.dragCandidateSpell = existing;
                        this.dragFromHotbar = true;
                        this.dragStartX = mouseX;
                        this.dragStartY = mouseY;
                        this.isDraggingSpell = false;
                        phb.playDownSound(mc.getSoundManager());
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.dragCandidateSpell != null) {
            if (Math.hypot(mouseX - this.dragStartX, mouseY - this.dragStartY) > 4.0D) {
                this.isDraggingSpell = true;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragCandidateSpell != null) {
            Spell spell = this.dragCandidateSpell;
            boolean wasDragging = this.isDraggingSpell || Math.hypot(mouseX - this.dragStartX, mouseY - this.dragStartY) > 4.0D;
            boolean fromHotbar = this.dragFromHotbar;

            this.dragCandidateSpell = null;
            this.isDraggingSpell = false;
            this.dragFromHotbar = false;

            for (Button b : this.buttons) {
                if (b instanceof PickHotbarButton phb && phb.isInside((int) mouseX, (int) mouseY)) {
                    if (!wasDragging && fromHotbar) {
                        IS_PICKING_HOTBAR_SPELL = true;
                        NUMBER_OF_HOTBAR_FOR_PICKING = phb.num;
                        setStatusMessage("Выберите изученное заклинание для ячейки #" + (phb.num + 1));
                        return true;
                    }
                    if (Load.spells(mc.player).getLevelOf(spell.GUID()) > 0) {
                        IS_PICKING_HOTBAR_SPELL = false;
                        Load.spells(mc.player).getSpellsData().hotbars.put(phb.num, spell.GUID());
                        Packets.sendToServer(new SetupHotbarPacket(spell, phb.num));
                        setStatusMessage("Заклинание назначено в ячейку #" + (phb.num + 1));
                    } else {
                        setStatusMessage("Сначала изучите заклинание, чтобы перетащить его на панель!");
                    }
                    return true;
                }
            }

            if (!wasDragging && !fromHotbar) {
                if (IS_PICKING_HOTBAR_SPELL) {
                    if (Load.spells(mc.player).getLevelOf(spell.GUID()) > 0) {
                        IS_PICKING_HOTBAR_SPELL = false;
                        Load.spells(mc.player).getSpellsData().hotbars.put(NUMBER_OF_HOTBAR_FOR_PICKING, spell.GUID());
                        Packets.sendToServer(new SetupHotbarPacket(spell, NUMBER_OF_HOTBAR_FOR_PICKING));
                        setStatusMessage("Заклинание назначено в ячейку #" + (NUMBER_OF_HOTBAR_FOR_PICKING + 1));
                    } else {
                        setStatusMessage("Сначала изучите это заклинание!");
                    }
                } else {
                    Packets.sendToServer(new AllocateSpellPacket(currentSchool(), spell, AllocateSpellPacket.ACTION.ALLOCATE));
                }
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack matrix, int x, int y, float ticks) {

        try {
            if (this.dragCandidateSpell != null && Math.hypot(x - this.dragStartX, y - this.dragStartY) > 4.0D) {
                this.isDraggingSpell = true;
            }

            AbstractGui.bindTexture(BACKGROUND);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            blit(matrix, mc.getWindow()
                    .getGuiScaledWidth() / 2 - sizeX / 2,
                mc.getWindow()
                    .getGuiScaledHeight() / 2 - sizeY / 2, 0, 0, sizeX, sizeY
            );

            AbstractGui.bindTexture(currentSchool().getIconLoc());
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            blit(matrix, guiLeft + 108, guiTop + 8, 34, 34, 0, 0, 34, 34, 34, 34);

            // background
            AbstractGui.bindTexture(currentSchool().getBackgroundLoc());
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            blit(matrix, guiLeft + 7, guiTop + 7, 93, 36, 0, 0, 93, 36, 93, 36);
            blit(matrix, guiLeft + 150, guiTop + 7, 93, 36, 0, 0, 93, 36, 93, 36);

            super.render(matrix, x, y, ticks);

            String txt = "Очки: " + Load.spells(mc.player)
                .getFreeSpellPoints();
            GuiUtils.renderScaledText(matrix, guiLeft + 125, guiTop + 215, 1D, txt, ChatFormatting.GREEN);

            String schoolTitle = getSchoolDisplayName(currentSchool());
            GuiUtils.renderScaledText(matrix, guiLeft + 42, guiTop + 25, 0.85D, schoolTitle, getSchoolColor(currentSchool()));

            if (!this.statusMessage.isEmpty() && System.currentTimeMillis() - this.statusMessageTime < 4000L) {
                GuiUtils.renderScaledText(matrix, guiLeft + sizeX / 2, guiTop + sizeY + 10, 0.9D, this.statusMessage, ChatFormatting.YELLOW);
            }

            if (this.isDraggingSpell && this.dragCandidateSpell != null) {
                for (Button b : this.buttons) {
                    if (b instanceof PickHotbarButton phb) {
                        int borderCol = phb.isInside(x, y) ? 0xFF55FF55 : 0xFFFFAA00;
                        int bx = phb.x;
                        int by = phb.y;
                        int bw = PickHotbarButton.BUTTON_SIZE_X;
                        int bh = PickHotbarButton.BUTTON_SIZE_Y;
                        AbstractGui.fill(matrix, bx - 1, by - 1, bx + bw + 1, by, borderCol);
                        AbstractGui.fill(matrix, bx - 1, by + bh, bx + bw + 1, by + bh + 1, borderCol);
                        AbstractGui.fill(matrix, bx - 1, by, bx, by + bh, borderCol);
                        AbstractGui.fill(matrix, bx + bw, by, bx + bw + 1, by + bh, borderCol);
                    }
                }
                AbstractGui.bindTexture(this.dragCandidateSpell.getIconLoc());
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                blit(matrix, x - 8, y - 8, 16, 16, 0, 0, 16, 16, 16, 16);
            } else {
                buttons.forEach(b -> b.renderToolTip(matrix, x, y));
                renderSchoolHeaderTooltip(matrix, x, y);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getSchoolDisplayName(SpellSchool school) {
        if (school == null) {
            return "";
        }
        return switch (school.GUID()) {
            case "divine" -> "Божественность";
            case "hunting" -> "Охота";
            case "nature" -> "Природа";
            case "fire" -> "Огонь";
            case "water" -> "Вода";
            default -> school.locName().getString();
        };
    }

    private String getSchoolSubtitle(SpellSchool school) {
        if (school == null) {
            return "";
        }
        return switch (school.GUID()) {
            case "divine" -> "Паладин / Жрец — свет, исцеление, ауры и мощные удары ближнего боя";
            case "hunting" -> "Охотник / Следопыт — стрельба из лука, ловушки, кровотечение и манёвренность";
            case "nature" -> "Друид / Шаман — силы природы, яды, шипы, тотемы и восстановление";
            case "fire" -> "Пиромант — разрушительная огненная магия, метеоры и поджоги";
            case "water" -> "Маг воды и льда — заморозка, контроль толпы и магия океана";
            default -> "Школа навыков и заклинаний";
        };
    }

    private ChatFormatting getSchoolColor(SpellSchool school) {
        if (school == null) {
            return ChatFormatting.GOLD;
        }
        return switch (school.GUID()) {
            case "divine" -> ChatFormatting.YELLOW;
            case "hunting" -> ChatFormatting.GREEN;
            case "nature" -> ChatFormatting.DARK_GREEN;
            case "fire" -> ChatFormatting.RED;
            case "water" -> ChatFormatting.AQUA;
            default -> ChatFormatting.GOLD;
        };
    }

    private void renderSchoolHeaderTooltip(PoseStack matrix, int mouseX, int mouseY) {
        int leftBtnX = guiLeft + 100 - LeftRightButton.xSize - 5;
        int rightBtnX = guiLeft + 150 + 5;
        int btnY = guiTop + 25 - LeftRightButton.ySize / 2;

        if (GuiUtils.isInRect(leftBtnX, btnY, LeftRightButton.xSize, LeftRightButton.ySize, mouseX, mouseY)) {
            int prevIdx = currentIndex - 1 < 0 ? maxIndex : currentIndex - 1;
            SpellSchool prev = schoolsInOrder.get(prevIdx);
            java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(net.minecraft.network.chat.Component.literal("Предыдущий класс: " + getSchoolDisplayName(prev)).withStyle(getSchoolColor(prev)));
            tooltip.add(net.minecraft.network.chat.Component.literal(getSchoolSubtitle(prev)).withStyle(ChatFormatting.GRAY));
            tooltip.add(net.minecraft.network.chat.Component.literal("ЛКМ — переключить школу навыков").withStyle(ChatFormatting.DARK_GRAY));
            GuiUtils.renderTooltip(matrix, tooltip, mouseX, mouseY);
            return;
        }

        if (GuiUtils.isInRect(rightBtnX, btnY, LeftRightButton.xSize, LeftRightButton.ySize, mouseX, mouseY)) {
            int nextIdx = currentIndex + 1 > maxIndex ? 0 : currentIndex + 1;
            SpellSchool next = schoolsInOrder.get(nextIdx);
            java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(net.minecraft.network.chat.Component.literal("Следующий класс: " + getSchoolDisplayName(next)).withStyle(getSchoolColor(next)));
            tooltip.add(net.minecraft.network.chat.Component.literal(getSchoolSubtitle(next)).withStyle(ChatFormatting.GRAY));
            tooltip.add(net.minecraft.network.chat.Component.literal("ЛКМ — переключить школу навыков").withStyle(ChatFormatting.DARK_GRAY));
            GuiUtils.renderTooltip(matrix, tooltip, mouseX, mouseY);
            return;
        }

        boolean overIcon = GuiUtils.isInRect(guiLeft + 108, guiTop + 8, 34, 34, mouseX, mouseY);
        boolean overLeftBanner = GuiUtils.isInRect(guiLeft + 7, guiTop + 7, 93, 36, mouseX, mouseY);
        boolean overRightBanner = GuiUtils.isInRect(guiLeft + 150, guiTop + 7, 93, 36, mouseX, mouseY);

        if (overIcon || overLeftBanner || overRightBanner) {
            SpellSchool school = currentSchool();
            java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(net.minecraft.network.chat.Component.literal("Класс: " + getSchoolDisplayName(school)).withStyle(getSchoolColor(school), ChatFormatting.BOLD));
            tooltip.add(net.minecraft.network.chat.Component.literal(getSchoolSubtitle(school)).withStyle(ChatFormatting.GRAY));
            int spent = Load.spells(mc.player).getSpellsData().getAllocatedPointsInSchool(school);
            tooltip.add(net.minecraft.network.chat.Component.literal("Вложено очков в этой школе: " + spent).withStyle(ChatFormatting.AQUA));
            tooltip.add(net.minecraft.network.chat.Component.literal("Стрелки влево/вправо переключают класс").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(net.minecraft.network.chat.Component.literal("Перетащите изученный навык на нижнюю панель").withStyle(ChatFormatting.YELLOW));
            GuiUtils.renderTooltip(matrix, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void goLeft() {
        this.currentIndex--;
        if (currentIndex < 0) {
            currentIndex = maxIndex;
        }
        init();
    }

    @Override
    public void goRight() {
        currentIndex++;
        if (currentIndex > maxIndex) {
            currentIndex = 0;
        }
        init();
    }
}
