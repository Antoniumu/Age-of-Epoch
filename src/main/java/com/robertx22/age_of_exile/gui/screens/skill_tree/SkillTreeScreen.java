package com.robertx22.age_of_exile.gui.screens.skill_tree;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.talent_tree.TalentTree;
import com.robertx22.age_of_exile.database.data.talent_tree.TalentTree.SchoolType;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.gui.screens.skill_tree.buttons.ConnectionButton;
import com.robertx22.age_of_exile.gui.screens.skill_tree.buttons.PerkButton;
import com.robertx22.age_of_exile.gui.screens.skill_tree.buttons.SelectTreeButton;
import com.robertx22.age_of_exile.gui.screens.skill_tree.buttons.TalentTreeButton;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.forge_compat.client.Screen;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.library_of_exile.utils.GuiUtils.PointF;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class SkillTreeScreen extends BaseScreen implements INamedScreen {
    static Identifier BIG_PANEL = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/skill_tree/background.png");
    static Identifier SMALL_PANEL = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/skill_tree/small_panel.png");

    public SchoolType schoolType;

    public SkillTreeScreen(SchoolType type) {
        super(Minecraft.getInstance()
            .getWindow()
            .getGuiScaledWidth(), Minecraft.getInstance()
            .getWindow()
            .getGuiScaledHeight());
        this.schoolType = type;

    }

    public PointData pointClicked = new PointData(0, 0);
    public int mouseRecentlyClickedTicks = 0;

    private final List<PerkButton> perkButtons = new ArrayList<>();
    private final List<ConnectionButton> connectionButtons = new ArrayList<>();
    private Object lastTalentsRef = null;
    private boolean dirtyStatuses = true;

    public void markDirty() {
        this.dirtyStatuses = true;
        this.mouseRecentlyClickedTicks = 25;
    }

    public String statusMessage = "";
    public int statusMessageTicks = 0;

    public void setStatusMessage(String msg) {
        this.statusMessage = msg != null ? msg : "";
        this.statusMessageTicks = this.statusMessage.isEmpty() ? 0 : 100;
    }

    @Override
    public boolean mouseReleased(double x, double y, int ticks) {

        mouseRecentlyClickedTicks = 25;

        return super.mouseReleased(x, y, ticks);

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32) { // space
            this.goToCenter();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);

    }

    int tick_count = 0;
    int scrollX = 0;
    int scrollY = 0;

    HashMap<Button, PointData> originalButtonLocMap = new HashMap<>();
    HashMap<PointData, PerkButton> pointPerkButtonMap = new HashMap<>();

    public List<TalentTree> schoolsInOrder;

    public TalentTree getSchoolByIndexAllowsOutOfBounds(int i) {
        if (i >= schoolsInOrder.size()) {
            return schoolsInOrder.get(i - schoolsInOrder.size());
        }
        if (i < 0) {
            return schoolsInOrder.get(schoolsInOrder.size() + i);
        }
        return schoolsInOrder.get(i);

    }

    public Minecraft mc = Minecraft.getInstance();

    RPGPlayerData playerData = Load.playerRPGData(mc.player);

    public TalentTree school;

    @Override
    protected void init() {
        super.init();

        try {
            this.playerData = Load.playerRPGData(mc.player);

            schoolsInOrder = ExileDB.TalentTrees()
                .getFiltered(x -> {
                    return x.getSchool_type() == this.schoolType;
                });
            schoolsInOrder.sort(Comparator.comparingInt(x -> x.order));

            this.school = schoolsInOrder.get(0);

            refreshButtons();

            goToCenter();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addButtonPublic(Button b) {
        this.addButton(b);
    }

    private void addConnections() {

        HashSet<PointData> def = new HashSet<>();

        Set<Set<PointData>> cons = new HashSet<>();

        for (PerkButton pb : this.perkButtons) {
            Set<PointData> connections = this.school.calcData.connections.getOrDefault(pb.point, def);

            int x1 = pb.origX + pb.getWidth() / 2;
            int y1 = pb.origY + pb.getHeight() / 2;

            int size = 6;
            float spacing = size + size / 2F;

            for (PointData p : connections) {

                if (cons.contains(Sets.newHashSet(p, pb.point))) {
                    continue;
                }

                cons.add(Sets.newHashSet(p, pb.point));

                PerkButton sb = this.pointPerkButtonMap.get(p);

                if (sb == null) {
                    continue;
                }

                int x2 = sb.origX + sb.getWidth() / 2;
                int y2 = sb.origY + sb.getHeight() / 2;

                List<PointF> points = GuiUtils.generateCurve(new PointF(x1, y1), new PointF(x2, y2), 360f, spacing + 2, true);

                for (PointF point : points) {

                    int x = (int) (point.x - ((float) size / 2));
                    int y = (int) (point.y - ((float) size / 2));

                    this.connectionButtons.add(new ConnectionButton(this, school, p, pb.point, x, y));

                }

            }
        }

    }

    public void refreshButtons() {

        originalButtonLocMap.clear();
        pointPerkButtonMap.clear();
        perkButtons.clear();
        connectionButtons.clear();
        this.buttons.clear();
        this.children.clear();

        this.scrollX = 0;
        this.scrollY = 0;
        this.dirtyStatuses = true;
        this.playerData = Load.playerRPGData(mc.player);

        for (Map.Entry<PointData, String> e : school.calcData.perks.entrySet()) {
            Perk perk = ExileDB.Perks()
                .get(e.getValue());

            if (perk == null) {
                continue;
            }

            try {
                // centers them if they are smaller than the biggest one
                int addx = (PerkButton.BIGGEST) / 2 - perk
                    .getType().width / 2;
                int addy = (PerkButton.BIGGEST) / 2 - perk
                    .getType().height / 2;

                int subx = PerkButton.BIGGEST / 2;
                int suby = PerkButton.BIGGEST / 2;

                Point pos = getPosForPoint(e.getKey());
                int x = pos.x + addx - subx + TalentTreeButton.XSIZE / 2;
                int y = pos.y + addy - suby + TalentTreeButton.YSIZE / 2;

                this.newButton(new PerkButton(this, playerData, school, e.getKey(), perk, x, y));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        int sx = mc.getWindow()
            .getGuiScaledWidth() / 2 - TalentTreeButton.XSIZE / 2;
        int sy = 0;

        if (this.schoolsInOrder.size() > 1) {

            this.addButton(new TalentTreeButton(this, school, sx, sy));

            int place = this.schoolsInOrder.indexOf(school);

            for (int i = 0; i < 2; i++) {
                int xadd = TalentTreeButton.XSIZE * (i + 1) + i + 1;

                int index1 = i + 1;
                int index2 = -i - 1;

                TalentTree school1 = getSchoolByIndexAllowsOutOfBounds(place + index1);
                TalentTree school2 = getSchoolByIndexAllowsOutOfBounds(place + index2);

                this.addButton(new TalentTreeButton(this, school1, sx + xadd, sy));
                this.addButton(new TalentTreeButton(this, school2, sx - xadd, sy));

                if (i == 1) {
                    this.addButton(new SelectTreeButton(this, SelectTreeButton.LeftOrRight.LEFT, sx - xadd - 15, sy + TalentTreeButton.YSIZE / 2 - SelectTreeButton.YSIZE / 2));
                    this.addButton(new SelectTreeButton(this, SelectTreeButton.LeftOrRight.RIGHT, sx + xadd + TalentTreeButton.XSIZE + 1, sy + TalentTreeButton.YSIZE / 2 - SelectTreeButton.YSIZE / 2));
                }
            }
        }

        addConnections();
        updateButtonPositions();
        refreshCachedStates();

    }

    private Point getPosForPoint(PointData point) {

        float halfx = mc.getWindow()
            .getGuiScaledWidth() / 2F;

        float halfy = mc.getWindow()
            .getGuiScaledHeight() / 2F;

        float x = (point.x - school.calcData.center.x) * PerkButton.SPACING + 2;
        float y = (point.y - school.calcData.center.y) * PerkButton.SPACING + 2;

        x -= TalentTreeButton.XSIZE / 2F;
        y -= TalentTreeButton.YSIZE / 2F;

        int tx = (int) (halfx + x);
        int ty = (int) (halfy + y);

        return new Point(tx, ty);

    }

    public void goToCenter() {
        this.scrollX = 0;
        this.scrollY = 0;
        updateButtonPositions();
    }

    private void newButton(Button b) {
        this.addButton(b);
        originalButtonLocMap.put(b, new PointData(b.x, b.y));
        if (b instanceof PerkButton pb) {
            this.perkButtons.add(pb);
            this.pointPerkButtonMap.put(pb.point, pb);
        }
    }

    public float zoom = 0.6F;

    public static boolean RETURN_ZOOM = false;

    @Override
    public void onClose() {

        super.onClose();

        resetZoom();

    }

    @Override
    public void removed() {
        super.removed();

        resetZoom();

    }

    private void resetZoom() {
        zoom = 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        updateButtonPositions();
        int mx = (int) (1F / zoom * event.x());
        int my = (int) (1F / zoom * event.y());
        System.out.println("[AOE-CLICK] SkillTreeScreen.mouseClicked: raw=(" + event.x() + "," + event.y() + ")"
            + " zoom=" + zoom + " zoomed=(" + mx + "," + my + ")"
            + " scroll=(" + scrollX + "," + scrollY + ")"
            + " screenSize=(" + this.width + "," + this.height + ")"
            + " perkButtons=" + this.perkButtons.size());
        boolean res = super.mouseClicked(event, doubleClick);
        if (!res && !this.perkButtons.isEmpty()) {
            PerkButton closest = null;
            double bestDist = Double.MAX_VALUE;
            for (PerkButton pb : this.perkButtons) {
                double cx = pb.x + pb.getWidth() / 2.0;
                double cy = pb.y + pb.getHeight() / 2.0;
                double d = Math.hypot(mx - cx, my - cy);
                if (d < bestDist) {
                    bestDist = d;
                    closest = pb;
                }
            }
            if (closest != null) {
                System.out.println("[AOE-CLICK] PerkButton MISS! Closest button: point=" + closest.point
                    + " perk=" + (closest.perk != null ? closest.perk.GUID() : "null")
                    + " pos=(" + closest.x + "," + closest.y + ") size=(" + closest.getWidth() + "," + closest.getHeight() + ")"
                    + " orig=(" + closest.origX + "," + closest.origY + ") dist=" + bestDist);
            }
        }
        return res;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (scroll < 0) {
            zoom -= 0.1F;
        }
        if (scroll > 0) {
            zoom += 0.1F;
        }

        this.zoom = Mth.clamp(zoom, 0.15F, 1);
        updateButtonPositions();

        return true;
    }

    public PointData getZoomedPosition(PointData pos) {
        return new PointData((int) this.zoom / pos.x, (int) this.zoom / pos.y);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.scrollX += 1F / zoom * deltaX;
        this.scrollY += 1F / zoom * deltaY;

        scrollY = Mth.clamp(scrollY, -3333, 3333);
        updateButtonPositions();

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void updateButtonPositions() {
        float addx = (1F / zoom - 1) * this.width / 2F + scrollX;
        float addy = (1F / zoom - 1) * this.height / 2F + scrollY;

        for (ConnectionButton cb : this.connectionButtons) {
            cb.x = (int) (cb.origX + addx);
            cb.y = (int) (cb.origY + addy);
        }
        for (PerkButton pb : this.perkButtons) {
            pb.x = (int) (pb.origX + addx);
            pb.y = (int) (pb.origY + addy);
        }
    }

    private void refreshCachedStates() {
        RPGPlayerData current = Load.playerRPGData(mc.player);
        if (current == null) {
            return;
        }
        this.playerData = current;
        this.lastTalentsRef = current.talents;
        this.dirtyStatuses = false;

        for (PerkButton pb : this.perkButtons) {
            pb.refreshStatus(current);
        }
        for (ConnectionButton cb : this.connectionButtons) {
            cb.refreshConnection(current);
        }
    }

    @Override
    public void render(PoseStack matrix, int x, int y, float ticks) {

        if (mouseRecentlyClickedTicks > 0) {
            mouseRecentlyClickedTicks--;
        }
        if (statusMessageTicks > 0) {
            statusMessageTicks--;
            if (statusMessageTicks == 0) {
                statusMessage = "";
            }
        }

        RPGPlayerData current = Load.playerRPGData(mc.player);
        if (current != null) {
            this.playerData = current;
            if (this.dirtyStatuses || current.talents != this.lastTalentsRef || (mouseRecentlyClickedTicks > 0 && tick_count % 5 == 0) || tick_count % 40 == 0) {
                refreshCachedStates();
            }
        }

        renderBackgroundDirt(this, 0);

        matrix.pushPose();
        matrix.scale(zoom, zoom, zoom);

        try {
            updateButtonPositions();

            int maxVisX = (int) (this.width / zoom) + 40;
            int maxVisY = (int) (this.height / zoom) + 40;

            com.robertx22.forge_compat.client.AbstractGui.bindTexture(ConnectionButton.ID);
            for (ConnectionButton cb : this.connectionButtons) {
                if (cb.x >= -16 && cb.y >= -16 && cb.x <= maxVisX && cb.y <= maxVisY) {
                    cb.renderButtonForReal(matrix, x, y, ticks);
                }
            }

            for (PerkButton pb : this.perkButtons) {
                if (pb.x + pb.getWidth() >= -16 && pb.y + pb.getHeight() >= -16 && pb.x <= maxVisX && pb.y <= maxVisY) {
                    pb.render(matrix, x, y, ticks);
                }
            }

            this.tick_count++;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            matrix.popPose();
        }

        renderPanels(matrix);
        for (Button button : buttons) {
            if (button instanceof IMarkOnTop) {
                button.render(matrix, x, y, ticks);
            }
        }

        int zoomedMouseX = (int) (1F / zoom * x);
        int zoomedMouseY = (int) (1F / zoom * y);
        for (PerkButton pb : this.perkButtons) {
            if (pb.isInside(zoomedMouseX, zoomedMouseY)) {
                pb.renderToolTip(matrix, x, y);
                break;
            }
        }
        for (Button b : buttons) {
            if (!(b instanceof PerkButton)) {
                b.renderToolTip(matrix, x, y);
            }
        }
    }

    private void renderPanels(PoseStack matrix) {

        int BG_HEIGHT = 38;
        Minecraft mc = Minecraft.getInstance();
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(BIG_PANEL);

        RenderSystem.enableDepthTest();

        int BG_WIDTH = 237;
        int xp = mc.getWindow()
            .getGuiScaledWidth() / 2 - BG_WIDTH / 2;
        int yp = 0;

        if (this.schoolsInOrder.size() > 1) {
            blit(matrix, xp, yp, 0, 0, BG_WIDTH, 39);
        }

        com.robertx22.forge_compat.client.AbstractGui.bindTexture(SMALL_PANEL);
        RenderSystem.enableDepthTest();

        int savedx = xp;
        int savedy = yp;

        // LEFT
        xp = savedx;
        yp = savedy;

        String text = "Очки: " + playerData.talents.getFreePoints(Load.Unit(mc.player), this.schoolType);

        int tx = xp - mc.font.width(text) - 10;
        int yx = yp + BG_HEIGHT / 2 - mc.font.lineHeight / 2;

        com.robertx22.forge_compat.client.AbstractGui.drawShadow(Minecraft.getInstance().font, matrix, text, tx, yx, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.GREEN));

        text = "Очки сброса: " + playerData.talents.reset_points;

        tx = savedx + 10 + BG_WIDTH;

        com.robertx22.forge_compat.client.AbstractGui.drawShadow(Minecraft.getInstance().font, matrix, text, tx, yx, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.GREEN));

        String topHint = this.statusMessage;
        int hintColor = com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.RED);
        if ((topHint == null || topHint.isEmpty()) && school != null && playerData != null && playerData.talents != null) {
            if (playerData.talents.getSchool(school).getAllocatedPoints(school).isEmpty()
                && playerData.talents.hasFreePoints(Load.Unit(mc.player), this.schoolType)) {
                topHint = "Выберите один из 6 начальных путей в центре древа";
                hintColor = com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.GOLD);
            }
        }
        if (topHint != null && !topHint.isEmpty()) {
            int hintX = mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(topHint) / 2;
            int hintY = this.schoolsInOrder.size() > 1 ? 42 : yx;
            com.robertx22.forge_compat.client.AbstractGui.drawShadow(Minecraft.getInstance().font, matrix, topHint, hintX, hintY, hintColor);
        }

    }

    static Identifier BACKGROUND = SlashRef.guiId("skill_tree/background");

    public static void renderBackgroundDirt(Screen screen, int vOffset) {
        PoseStack emptyPose = new PoseStack();
        com.robertx22.forge_compat.client.AbstractGui.fill(emptyPose, 0, 0, screen.width, screen.height, 0xFF141018);
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(BACKGROUND);
        com.robertx22.forge_compat.client.AbstractGui.blit(emptyPose, 0, 0, 0.0F, (float) vOffset, screen.width, screen.height, 32, 32);
    }

}
