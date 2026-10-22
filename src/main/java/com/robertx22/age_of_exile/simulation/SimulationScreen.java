package com.robertx22.age_of_exile.simulation;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.Armor;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.DodgeRating;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.TalentsScreen;
import com.robertx22.mine_and_slash.gui.screens.spell.SpellSchoolScreen;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class SimulationScreen extends BaseScreen implements INamedScreen {

    public SimulationScreen() {
        super(360, 224);
    }

    @Override
    public Identifier iconLocation() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/stats.png");
    }

    @Override
    public Words screenName() {
        return Words.Simulation;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int x1 = this.guiLeft + 10;
        int x2 = this.guiLeft + 184;
        int y = this.guiTop + 48;

        publicAddButton(Button.builder(Component.literal("§a▶ Войти в Симуляцию"), b ->
            Packets.sendToServer(new SimulationPacket("enter", 0))).bounds(x1, y, 166, 20).build());
        publicAddButton(Button.builder(Component.literal("§c◀ Выйти (вернуть сейв)"), b ->
            Packets.sendToServer(new SimulationPacket("exit", 0))).bounds(x2, y, 166, 20).build());

        publicAddButton(Button.builder(Component.literal("Ур. 1"), b ->
            Packets.sendToServer(new SimulationPacket("level", 1))).bounds(x1, y + 26, 52, 20).build());
        publicAddButton(Button.builder(Component.literal("Ур. 50"), b ->
            Packets.sendToServer(new SimulationPacket("level", 50))).bounds(x1 + 57, y + 26, 52, 20).build());
        publicAddButton(Button.builder(Component.literal("Ур. 100"), b ->
            Packets.sendToServer(new SimulationPacket("level", 100))).bounds(x1 + 114, y + 26, 52, 20).build());

        publicAddButton(Button.builder(Component.literal("§dВыдать Тест-Кит (Шмот/Сферы)"), b ->
            Packets.sendToServer(new SimulationPacket("kit", 0))).bounds(x2, y + 26, 166, 20).build());

        publicAddButton(Button.builder(Component.literal("§eСпавн Манекена"), b ->
            Packets.sendToServer(new SimulationPacket("dummy", 0))).bounds(x1, y + 52, 166, 20).build());
        publicAddButton(Button.builder(Component.literal("§6Спавн Манекена-Босса"), b ->
            Packets.sendToServer(new SimulationPacket("dummy", 1))).bounds(x2, y + 52, 166, 20).build());

        publicAddButton(Button.builder(Component.literal("§bТест физ. удара"), b ->
            Packets.sendToServer(new SimulationPacket("hit", 0))).bounds(x1, y + 78, 166, 20).build());
        publicAddButton(Button.builder(Component.literal("§3Тест маг. удара"), b ->
            Packets.sendToServer(new SimulationPacket("hit", 2))).bounds(x2, y + 78, 166, 20).build());

        publicAddButton(Button.builder(Component.literal("§cСбросить все очки"), b ->
            Packets.sendToServer(new SimulationPacket("reset", 0))).bounds(x1, y + 104, 166, 20).build());
        publicAddButton(Button.builder(Component.literal("§aПолное исцеление"), b ->
            Packets.sendToServer(new SimulationPacket("heal", 0))).bounds(x2, y + 104, 166, 20).build());

        int navY = this.guiTop + this.sizeY - 28;
        publicAddButton(Button.builder(Component.literal("§e← Меню"), b ->
            Minecraft.getInstance().gui.setScreen(new MainHubScreen())).bounds(x1, navY, 80, 20).build());
        publicAddButton(Button.builder(Component.literal("§eТаланты"), b ->
            Minecraft.getInstance().gui.setScreen(new TalentsScreen())).bounds(x1 + 86, navY, 84, 20).build());
        publicAddButton(Button.builder(Component.literal("§bСпеллы"), b ->
            Minecraft.getInstance().gui.setScreen(new SpellSchoolScreen())).bounds(x1 + 176, navY, 84, 20).build());
        publicAddButton(Button.builder(Component.literal("§aСтаты"), b ->
            Minecraft.getInstance().gui.setScreen(new StatScreen())).bounds(x1 + 266, navY, 74, 20).build());
    }

    @Override
    public void render(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTicks) {
        gui.fill(this.guiLeft, this.guiTop, this.guiLeft + this.sizeX, this.guiTop + this.sizeY, 0xE614141C);
        gui.fill(this.guiLeft, this.guiTop, this.guiLeft + this.sizeX, this.guiTop + 2, 0xFFFFAA00);

        EntityData ud = Load.Unit(mc.player);
        Unit u = ud.getUnit();
        int lvl = ud.getLevel();
        float armor = u.getCalculatedStat(Armor.getInstance()).getValue();
        float armorPct = Armor.getInstance().getUsableValue(u, (int) armor, lvl) * 100F;
        float dodge = u.getCalculatedStat(DodgeRating.getInstance()).getValue();
        float dodgePct = DodgeRating.getInstance().getUsableValue(u, (int) dodge, lvl) * 100F;

        gui.text(mc.font, "§6Песочница Симуляции Билдов Age of Epoch", this.guiLeft + 10, this.guiTop + 9, 0xFFFFFFFF, true);
        gui.text(mc.font, String.format("§7Ур: §f%d  §8|  §7Броня: §f%.0f (%.1f%%)  §8|  §7Уклон: §f%.0f (%.1f%%)",
            lvl, armor, armorPct, dodge, dodgePct), this.guiLeft + 10, this.guiTop + 26, 0xFFCCCCCC, false);

        super.extractRenderState(gui, mouseX, mouseY, partialTicks);
    }
}
