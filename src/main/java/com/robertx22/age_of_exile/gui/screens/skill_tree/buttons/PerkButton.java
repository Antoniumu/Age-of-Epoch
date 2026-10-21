package com.robertx22.age_of_exile.gui.screens.skill_tree.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.perks.PerkStatus;
import com.robertx22.age_of_exile.database.data.talent_tree.TalentTree;
import com.robertx22.age_of_exile.gui.screens.skill_tree.SkillTreeScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.vanilla_mc.packets.perks.PerkChangePacket;
import com.robertx22.forge_compat.client.ImageButton;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.library_of_exile.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class PerkButton extends ImageButton {

    public static int SPACING = 26;
    public static int BIGGEST = 33;

    public static Identifier ID = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/skill_tree/perk_buttons.png");
    public static Identifier LOCKED_TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/locked.png");

    public Perk perk;
    public PointData point;
    public TalentTree school;
    public RPGPlayerData playerData;

    public int originalWidth;
    public int originalHeight;

    public int origX;
    public int origY;
    Minecraft mc = Minecraft.getInstance();
    SkillTreeScreen screen;

    public PerkStatus cachedStatus = null;
    private Identifier cachedSchoolIcon = null;

    public PerkButton(SkillTreeScreen screen, RPGPlayerData playerData, TalentTree school, PointData point, Perk perk, int x, int y) {
        super(x, y, perk.getType().width, perk.getType().height, 0, 0, 1, ID, (action) -> {
        });
        this.perk = perk;
        this.point = point;
        this.school = school;
        this.playerData = playerData;

        this.origX = x;
        this.origY = y;
        this.originalWidth = this.width;
        this.originalHeight = this.height;
        this.screen = screen;
    }

    public void refreshStatus(RPGPlayerData data) {
        if (data != null && data.talents != null && mc.player != null) {
            this.playerData = data;
            this.cachedStatus = data.talents.getStatus(mc.player, school, point);
        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, this.width, this.height, x, y);
    }

    @Override
    public void renderToolTip(PoseStack matrices, int mouseX, int mouseY) {
        int MmouseX = (int) (1F / screen.zoom * mouseX);
        int MmouseY = (int) (1F / screen.zoom * mouseY);

        if (this.isInside(MmouseX, MmouseY)) {
            List<Component> tooltip = new java.util.ArrayList<>(perk.GetTooltipString(new TooltipInfo(Minecraft.getInstance().player)));
            RPGPlayerData data = Load.playerRPGData(mc.player);
            if (data != null && data.talents != null && mc.player != null) {
                PerkStatus status = cachedStatus != null ? cachedStatus : data.talents.getStatus(mc.player, school, point);
                if (status == PerkStatus.POSSIBLE) {
                    tooltip.add(Component.literal("ЛКМ / ПКМ — Изучить талант").withStyle(net.minecraft.ChatFormatting.GREEN));
                } else if (status == PerkStatus.CONNECTED) {
                    if (data.talents.canRemove(school, point)) {
                        tooltip.add(Component.literal("Изучено (ПКМ — сбросить, очков сброса: " + data.talents.reset_points + ")").withStyle(net.minecraft.ChatFormatting.AQUA));
                    } else if (!data.talents.canRemoveIgnoringResetPoints(school, point)) {
                        tooltip.add(Component.literal("Изучено (сначала сбросьте крайние таланты)").withStyle(net.minecraft.ChatFormatting.GRAY));
                    } else {
                        tooltip.add(Component.literal("Изучено (нет очков сброса)").withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                } else if (data.talents.findAllocatableEntryConnection(school, point, Load.Unit(mc.player), mc.player) != null) {
                    tooltip.add(Component.literal("ЛКМ / ПКМ — Выбрать этот начальный путь").withStyle(net.minecraft.ChatFormatting.GOLD));
                } else {
                    String reason = data.talents.getFailAllocateReason(school, point, Load.Unit(mc.player), mc.player);
                    if (reason != null && !reason.isEmpty()) {
                        tooltip.add(Component.literal(reason).withStyle(net.minecraft.ChatFormatting.RED));
                    }
                }
            }
            GuiUtils.renderTooltip(matrices, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) (1F / screen.zoom * mouseX);
        int my = (int) (1F / screen.zoom * mouseY);

        if (this.active && this.visible && this.isInside(mx, my)) {
            screen.mouseRecentlyClickedTicks = 25;
            screen.pointClicked = this.point;

            RPGPlayerData currentData = Load.playerRPGData(mc.player);
            if (currentData != null) {
                this.playerData = currentData;
            }

            com.robertx22.age_of_exile.capability.entity.EntityData unitData = Load.Unit(mc.player);
            boolean canAlloc = this.playerData != null && this.playerData.talents.canAllocate(school, point, unitData, mc.player);
            String failReason = this.playerData != null ? this.playerData.talents.getFailAllocateReason(school, point, unitData, mc.player) : "playerData is null";
            System.out.println("[AOE-CLICK] PerkButton HIT: point=" + point
                + " perk=" + (perk != null ? perk.GUID() : "null")
                + " type=" + (perk != null ? perk.getType() : "null")
                + " isEntry=" + (perk != null && perk.is_entry)
                + " oneOfAKind=" + (perk != null ? perk.one_of_a_kind : "null")
                + " lvlReq=" + (perk != null ? perk.lvl_req : -1)
                + " btn=" + button
                + " playerLvl=" + (unitData != null ? unitData.getLevel() : -1)
                + " freePoints=" + (this.playerData != null && unitData != null ? this.playerData.talents.getFreePoints(unitData, school.getSchool_type()) : -999)
                + " allocatedInSchool=" + (this.playerData != null ? this.playerData.talents.getSchool(school).getAllocatedPoints(school) : "null")
                + " connections=" + (school != null && school.calcData != null ? school.calcData.connections.get(point) : "null")
                + " canAllocate=" + canAlloc
                + " failReason=" + failReason);

            this.playDownSound(Minecraft.getInstance().getSoundManager());

            if (this.playerData != null) {
                boolean alreadyAllocated = this.playerData.talents.isAllocated(school, point);
                if (!alreadyAllocated && (button == 0 || button == 1)) {
                    if (this.playerData.talents.canAllocate(school, point, Load.Unit(mc.player), mc.player)) {
                        this.playerData.talents.allocate(mc.player, school, point);
                        screen.setStatusMessage("");
                        Packets.sendToServer(new PerkChangePacket(school, point, PerkChangePacket.ACTION.ALLOCATE));
                    } else {
                        PointData entryPoint = this.playerData.talents.findAllocatableEntryConnection(school, point, Load.Unit(mc.player), mc.player);
                        if (entryPoint != null) {
                            this.playerData.talents.allocate(mc.player, school, entryPoint);
                            if (this.playerData.talents.canAllocate(school, point, Load.Unit(mc.player), mc.player)) {
                                this.playerData.talents.allocate(mc.player, school, point);
                                screen.setStatusMessage("");
                            } else {
                                screen.setStatusMessage("Начальный путь выбран! Для следующего узла нужны очки талантов.");
                            }
                            Packets.sendToServer(new PerkChangePacket(school, point, PerkChangePacket.ACTION.ALLOCATE));
                        } else {
                            String reason = this.playerData.talents.getFailAllocateReason(school, point, Load.Unit(mc.player), mc.player);
                            screen.setStatusMessage(reason);
                        }
                    }
                    screen.markDirty();
                } else if (alreadyAllocated && button == 1) {
                    if (this.playerData.talents.canRemove(school, point)) {
                        this.playerData.talents.remove(school, point);
                        this.playerData.talents.reset_points--;
                        screen.setStatusMessage("");
                        Packets.sendToServer(new PerkChangePacket(school, point, PerkChangePacket.ACTION.REMOVE));
                    } else {
                        if (this.playerData.talents.reset_points < 1) {
                            screen.setStatusMessage("Недостаточно очков сброса");
                        } else {
                            screen.setStatusMessage("Нельзя сбросить: от этого таланта зависят другие изученные узлы");
                        }
                    }
                    screen.markDirty();
                } else if (alreadyAllocated && button == 0) {
                    screen.setStatusMessage("Талант уже изучен (ПКМ — сбросить)");
                }
            }
            this.onClick(mx, my);
            return true;
        }
        return false;
    }

    @Override
    public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
        if (cachedStatus == null) {
            refreshStatus(Load.playerRPGData(mc.player));
        }
        PerkStatus status = cachedStatus != null ? cachedStatus : PerkStatus.BLOCKED;

        com.robertx22.forge_compat.client.AbstractGui.bindTexture(ID);

        RenderSystem.enableDepthTest();
        blit(matrices, this.x, this.y, perk.getType().getXOffset(), status.getYOffset(), this.width, this.height);

        int offset = (this.width - 16) / 2;
        int offsetY = (this.height - 16) / 2;

        if (perk.getType() == Perk.PerkType.START && (perk.icon == null || perk.icon.isEmpty())) {
            if (cachedSchoolIcon == null) {
                cachedSchoolIcon = Identifier.parse(school.icon);
            }
            RenderUtils.render16Icon(matrices, cachedSchoolIcon, this.x + offset, this.y + offsetY);
        } else {
            RenderUtils.render16Icon(matrices, this.perk.getIcon(), this.x + offset, this.y + offsetY);
        }
    }
}
