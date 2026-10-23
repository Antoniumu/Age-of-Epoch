package com.robertx22.age_of_exile.simulation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.stats.types.defense.Armor;
import com.robertx22.age_of_exile.database.data.stats.types.defense.DodgeRating;
import com.robertx22.age_of_exile.database.data.stats.types.defense.SpellDodge;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.gui.screens.character_screen.CharacterScreen;
import com.robertx22.age_of_exile.gui.screens.skill_tree.TalentsScreen;
import com.robertx22.age_of_exile.gui.screens.spell.SpellScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.unit.Unit;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.forge_compat.common.CompatChatFormatting;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimulationScreen extends BaseScreen implements INamedScreen {

    private static final Identifier PANEL_BG = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/full_stats_panel.png");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/wiki.png");

    public SimulationScreen() {
        super(256, 180);
    }

    @Override
    public Identifier iconLocation() {
        return ICON;
    }

    @Override
    public Words screenName() {
        return Words.Simulation;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.children.clear();

        Minecraft mc = Minecraft.getInstance();
        boolean inSim = mc.player != null && WorldUtils.isSimulationWorld(mc.player.level());
        int curLvl = mc.player != null ? Load.Unit(mc.player).getLevel() : 1;

        int left = guiLeft + 8;
        int y = guiTop + 34;
        int rowH = 21;

        // Row 1: Enter/Exit, Bonus Points, Reset Build
        addButton(new SimButton(
            left, y, 96, 18,
            inSim ? "§cВыйти (Вернуть всё)" : "§aВойти на Полигон",
            b -> {
                Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.TOGGLE_SIMULATION));
                onClose();
            },
            inSim
                ? Arrays.asList(
                    Component.literal("Выйти с Полигона Симуляции").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal("100% восстанавливает ваш исходный уровень,").withStyle(ChatFormatting.GRAY),
                    Component.literal("инвентарь, таланты, заклинания и координаты.").withStyle(ChatFormatting.GRAY)
                )
                : Arrays.asList(
                    Component.literal("Войти в измерение Симуляции").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.literal("Сохраняет полный снимок вашего персонажа и").withStyle(ChatFormatting.GRAY),
                    Component.literal("переносит вас на арену для теста любых билдов.").withStyle(ChatFormatting.GRAY)
                )
        ));

        addButton(new SimButton(
            left + 99, y, 70, 18,
            "§e+50 Очков",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.GRANT_POINTS)),
            Arrays.asList(
                Component.literal("Выдать очки для теста").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                Component.literal("+50 бонусных очков талантов, 999 очков сброса").withStyle(ChatFormatting.GRAY),
                Component.literal("и +50 ко всем базовым характеристикам.").withStyle(ChatFormatting.GRAY)
            )
        ));

        addButton(new SimButton(
            left + 172, y, 68, 18,
            "§6Сброс билда",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.RESET_BUILD)),
            Arrays.asList(
                Component.literal("Полный сброс билда на полигоне").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Сбрасывает все изученные таланты, характеристики").withStyle(ChatFormatting.GRAY),
                Component.literal("и заклинания для сборки нового билда с нуля.").withStyle(ChatFormatting.GRAY)
            )
        ));

        // Row 2: Level selection
        y += rowH;
        int[] presetLevels = new int[]{1, 10, 25, 40, 50};
        int bx = left;
        for (int preset : presetLevels) {
            final int targetLvl = preset;
            addButton(new SimButton(
                bx, y, 34, 18,
                "Ур." + targetLvl,
                b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.SET_LEVEL, targetLvl)),
                Arrays.asList(
                    Component.literal("Установить уровень: " + targetLvl).withStyle(ChatFormatting.AQUA),
                    Component.literal("Пересчитывает все характеристики и скалирование.").withStyle(ChatFormatting.GRAY)
                )
            ));
            bx += 36;
        }
        addButton(new SimButton(
            bx, y, 28, 18,
            "-1",
            b -> {
                int l = Minecraft.getInstance().player != null ? Load.Unit(Minecraft.getInstance().player).getLevel() : curLvl;
                Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.SET_LEVEL, Math.max(1, l - 1)));
            },
            List.of(Component.literal("Понизить уровень на 1").withStyle(ChatFormatting.YELLOW))
        ));
        addButton(new SimButton(
            bx + 30, y, 30, 18,
            "+1",
            b -> {
                int l = Minecraft.getInstance().player != null ? Load.Unit(Minecraft.getInstance().player).getLevel() : curLvl;
                Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.SET_LEVEL, l + 1));
            },
            List.of(Component.literal("Повысить уровень на 1").withStyle(ChatFormatting.GREEN))
        ));

        // Row 3: Gear & Trinket Kits
        y += rowH;
        addButton(new SimButton(
            left, y, 58, 18,
            "§cВоин",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.GIVE_GEAR_KIT, 0)),
            Arrays.asList(
                Component.literal("Набор экипировки: Воин").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.literal("Латная броня, меч, топор и тяжёлый щит").withStyle(ChatFormatting.GRAY),
                Component.literal("под ваш текущий уровень.").withStyle(ChatFormatting.GRAY)
            )
        ));
        addButton(new SimButton(
            left + 60, y, 58, 18,
            "§aОхотник",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.GIVE_GEAR_KIT, 1)),
            Arrays.asList(
                Component.literal("Набор экипировки: Охотник").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal("Кожаная броня на уклонение, лук, арбалет").withStyle(ChatFormatting.GRAY),
                Component.literal("и лёгкий щит под ваш текущий уровень.").withStyle(ChatFormatting.GRAY)
            )
        ));
        addButton(new SimButton(
            left + 120, y, 54, 18,
            "§bМаг",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.GIVE_GEAR_KIT, 2)),
            Arrays.asList(
                Component.literal("Набор экипировки: Маг").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.literal("Тканевая броня на Магический щит, посох,").withStyle(ChatFormatting.GRAY),
                Component.literal("жезл и щит мага под ваш текущий уровень.").withStyle(ChatFormatting.GRAY)
            )
        ));
        addButton(new SimButton(
            left + 176, y, 64, 18,
            "§dБижутерия",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.GIVE_GEAR_KIT, 3)),
            Arrays.asList(
                Component.literal("Набор: Кольца, Ожерелье, Души и Валюта").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Component.literal("Выдаёт 2 кольца, ожерелье, Души характеристик").withStyle(ChatFormatting.GRAY),
                Component.literal("и полный набор валюты модификации.").withStyle(ChatFormatting.GRAY)
            )
        ));

        // Row 4: Target Dummy & Mob Spawning
        y += rowH;
        addButton(new SimButton(
            left, y, 56, 18,
            "§6Манекен",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.SPAWN_DUMMY, 0)),
            Arrays.asList(
                Component.literal("Спавн Манекена ДПС").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Создаёт неподвижный манекен вашего уровня").withStyle(ChatFormatting.GRAY),
                Component.literal("с повышенным запасом ХП для проверки урона.").withStyle(ChatFormatting.GRAY)
            )
        ));
        addButton(new SimButton(
            left + 58, y, 44, 18,
            "Обычн.",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.SPAWN_TEST_MOB, 0, 0)),
            List.of(Component.literal("Спавн обычного моба вашего уровня").withStyle(ChatFormatting.WHITE))
        ));
        addButton(new SimButton(
            left + 104, y, 44, 18,
            "§bРедкий",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.SPAWN_TEST_MOB, 0, 2)),
            List.of(Component.literal("Спавн редкого моба вашего уровня").withStyle(ChatFormatting.AQUA))
        ));
        addButton(new SimButton(
            left + 150, y, 44, 18,
            "§dМифич.",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.SPAWN_TEST_MOB, 0, 5)),
            List.of(Component.literal("Спавн мифического моба вашего уровня").withStyle(ChatFormatting.LIGHT_PURPLE))
        ));
        addButton(new SimButton(
            left + 196, y, 44, 18,
            "§cОчист.",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.CLEAR_MOBS)),
            List.of(Component.literal("Удалить всех тестовых мобов и манекены").withStyle(ChatFormatting.RED))
        ));

        // Row 5: Defense / Dodge Stress Tests & Mob Scaling / Audit
        y += rowH;
        addButton(new SimButton(
            left, y, 46, 18,
            "🛡 Физ.",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.TEST_DEFENSE_BURST, 0, 0)),
            Arrays.asList(
                Component.literal("Стресс-тест: 20 ударов ближнего боя").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                Component.literal("Проверяет уклонение (Dodge), броню, Маг. щит").withStyle(ChatFormatting.GRAY),
                Component.literal("и поглощение урона маной (отчёт в чате).").withStyle(ChatFormatting.GRAY)
            )
        ));
        addButton(new SimButton(
            left + 48, y, 46, 18,
            "🏹 Лук",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.TEST_DEFENSE_BURST, 0, 1)),
            Arrays.asList(
                Component.literal("Стресс-тест: 20 выстрелов снарядами").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal("Проверяет уклонение от снарядов и защиту.").withStyle(ChatFormatting.GRAY)
            )
        ));
        addButton(new SimButton(
            left + 96, y, 46, 18,
            "🔥 Чары",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.TEST_DEFENSE_BURST, 0, 2)),
            Arrays.asList(
                Component.literal("Стресс-тест: 20 огненных заклинаний").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.literal("Проверяет уклонение от заклинаний (Spell Dodge),").withStyle(ChatFormatting.GRAY),
                Component.literal("Фазовую акробатику и стихийные сопротивления.").withStyle(ChatFormatting.GRAY)
            )
        ));
        addButton(new SimButton(
            left + 144, y, 46, 18,
            "📊 ХП",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.PRINT_MOB_SCALING_TABLE)),
            Arrays.asList(
                Component.literal("Таблица скалирования ХП и статов мобов").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Выводит в чат точные значения ХП, брони и урона").withStyle(ChatFormatting.GRAY),
                Component.literal("мобов для уровней 1–50 и всех редкостей.").withStyle(ChatFormatting.GRAY)
            )
        ));
        addButton(new SimButton(
            left + 192, y, 48, 18,
            "✔ Аудит",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.RUN_FULL_AUDIT)),
            Arrays.asList(
                Component.literal("Полный аудит талантов и статов").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal("Проверяет все перки дерева талантов, ключевые").withStyle(ChatFormatting.GRAY),
                Component.literal("узлы, бижутерию и итоговые статы персонажа.").withStyle(ChatFormatting.GRAY)
            )
        ));

        // Row 6: Quick navigation to Talents / Spells / Character Screen
        y += rowH;
        addButton(new SimButton(
            left, y, 78, 18,
            "Дерево талантов",
            b -> Minecraft.getInstance().gui.setScreen(new TalentsScreen()),
            List.of(Component.literal("Открыть Дерево талантов (свободный сброс ПКМ)").withStyle(ChatFormatting.YELLOW))
        ));
        addButton(new SimButton(
            left + 81, y, 78, 18,
            "Заклинания",
            b -> Minecraft.getInstance().gui.setScreen(new SpellScreen()),
            List.of(Component.literal("Открыть Экран заклинаний и хотбара").withStyle(ChatFormatting.AQUA))
        ));
        addButton(new SimButton(
            left + 162, y, 78, 18,
            "Персонаж и статы",
            b -> Minecraft.getInstance().gui.setScreen(new CharacterScreen()),
            List.of(Component.literal("Вернуться к Экрану персонажа").withStyle(ChatFormatting.GREEN))
        ));

        // Row 7: гайды — на полигоне гайд превращается в готовую сборку
        y += 20;
        String activeGuide = com.robertx22.age_of_exile.guides.GuideAdvisor.activeGuideId(mc.player);
        com.robertx22.age_of_exile.guides.GuideDefinition guide =
            com.robertx22.age_of_exile.guides.GuideData.byId(activeGuide);
        addButton(new SimButton(
            left, y, 100, 18,
            "§6Собрать гайд",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.GUIDE_BUILD, 1)),
            Arrays.asList(
                Component.literal(guide == null ? "Гайд не выбран" : "Гайд: " + guide.name)
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Сбрасывает билд и вкладывает статы, таланты,").withStyle(ChatFormatting.GRAY),
                Component.literal("заклинания и выдаёт экипировку под гайд.").withStyle(ChatFormatting.GRAY),
                Component.literal("Если гайд не выбран — берётся первый из списка.").withStyle(ChatFormatting.DARK_GRAY)
            )
        ));
        addButton(new SimButton(
            left + 104, y, 56, 18,
            "§eГайды",
            b -> Minecraft.getInstance().gui.setScreen(new com.robertx22.age_of_exile.guides.GuidesScreen()),
            List.of(Component.literal("Открыть вкладку гайдов").withStyle(ChatFormatting.YELLOW))
        ));
        addButton(new SimButton(
            left + 160, y, 80, 18,
            "§6Сброс+гайд",
            b -> Packets.sendToServer(new SimulationPacket(SimulationPacket.Action.RESET_AND_GUIDE)),
            List.of(Component.literal("Сброс и сборка активного гайда").withStyle(ChatFormatting.GOLD))
        ));
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.bindTexture(PANEL_BG);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        // текстура панели — 256×178; ниже 178 прозрачные пиксели дали бы щель внизу рамки
        blit(matrix, guiLeft, guiTop, 0, 0, sizeX, 178);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            boolean inSim = WorldUtils.isSimulationWorld(mc.player.level());
            EntityData unitData = Load.Unit(mc.player);
            int lvl = unitData.getLevel();
            Unit u = unitData.getUnit();

            float hp = u.healthData().getValue();
            float mana = u.manaData().getValue();
            float ms = u.magicShieldData().getValue();
            float armor = u.armorData().getValue();
            float armorPct = Armor.getInstance().getUsableValue((int) armor, lvl) * 100F;
            float dodge = u.dodgeData().getValue();
            float dodgePct = DodgeRating.getInstance().getUsableValue((int) dodge, lvl) * 100F;
            float spellDodge = u.getCalculatedStat(SpellDodge.getInstance()).getValue();
            float spellDodgePct = SpellDodge.getInstance().getUsableValue((int) spellDodge, lvl) * 100F;

            String title = inSim
                ? "§a● ПОЛИГОН СИМУЛЯЦИИ АКТИВЕН (Ур. " + lvl + ")"
                : "§e○ ПОЛИГОН СИМУЛЯЦИИ БИЛДОВ (Ур. " + lvl + ")";
            AbstractGui.drawShadow(
                mc.font, matrix, title,
                guiLeft + 10, guiTop + 8,
                CompatChatFormatting.getColor(ChatFormatting.WHITE)
            );

            String summary = "§cХП:" + (int) hp
                + " §bМана:" + (int) mana
                + " §dЩит:" + (int) ms
                + " §eБр:" + (int) armorPct + "%"
                + " §aУкл:" + (int) dodgePct + "%/" + (int) spellDodgePct + "%";
            AbstractGui.drawShadow(
                mc.font, matrix, summary,
                guiLeft + 10, guiTop + 20,
                CompatChatFormatting.getColor(ChatFormatting.GRAY)
            );
        }

        super.render(matrix, mouseX, mouseY, partialTicks);

        for (Button b : this.buttons) {
            b.renderToolTip(matrix, mouseX, mouseY);
        }
    }

    private static class SimButton extends Button {
        private final List<Component> tooltip;

        public SimButton(int x, int y, int w, int h, String label, OnPress onPress, List<Component> tooltip) {
            super(x, y, w, h, Component.literal(label), onPress);
            this.tooltip = tooltip != null ? tooltip : new ArrayList<>();
        }

        @Override
        public void renderToolTip(PoseStack matrix, int mouseX, int mouseY) {
            if (isHovered() && !tooltip.isEmpty()) {
                GuiUtils.renderTooltip(matrix, tooltip, mouseX, mouseY);
            }
        }
    }
}
