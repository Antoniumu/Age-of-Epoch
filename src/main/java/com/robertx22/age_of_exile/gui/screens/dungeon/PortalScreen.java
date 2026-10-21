package com.robertx22.age_of_exile.gui.screens.dungeon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.tiers.base.Difficulty;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.dimension.dungeon_data.DungeonData;
import com.robertx22.age_of_exile.dimension.dungeon_data.DungeonType;
import com.robertx22.age_of_exile.dimension.dungeon_data.TeamSize;
import com.robertx22.age_of_exile.dimension.dungeon_data.TeamSizeMobStrength;
import com.robertx22.age_of_exile.dimension.packets.StartDungeonPacket;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.CLOC;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран портала в подземелье — полностью переписан.
 *
 * <p>Старый {@code DungeonInfoScreen} рисовал 123×235 картинку, складывал кнопки
 * «в кучу» и не имел обработчиков нажатия, поэтому выбрать сложность было
 * невозможно. Здесь два столбца:</p>
 * <ul>
 *     <li>слева — список сложностей (заблокированные затемнены) и выбор состава отряда;</li>
 *     <li>справа — характеристики выбранной сложности и итоговые множители отряда.</li>
 * </ul>
 *
 * <p>Старт отправляет {@link StartDungeonPacket} с выбранной сложностью; сервер
 * сам создаёт подземелье через {@code RPGPlayerData#createRandomDungeon}.</p>
 */
public class PortalScreen extends BaseScreen {

    private static final Identifier PANEL = SlashRef.guiId("guides_panel");
    private static final int TEX_W = 300;
    private static final int TEX_H = 220;

    static final int SIZE_X = 300;
    static final int SIZE_Y = 220;

    private static final int LINE = 10;

    private static final int LEFT_X = 16;
    private static final int LEFT_W = 126;
    private static final int RIGHT_X = 150;
    private static final int RIGHT_W = 134;
    private static final int TOP_Y = 36;
    private static final int BAR_Y = SIZE_Y - 30;

    private final BlockPos teleporterPos;

    private final List<Difficulty> difficulties = new ArrayList<>();
    private String selectedDiff = "";
    private TeamSize selectedTeam = TeamSize.SOLO;

    public PortalScreen(BlockPos teleporterPos) {
        super(SIZE_X, SIZE_Y);
        this.teleporterPos = teleporterPos;

        try {
            difficulties.addAll(ExileDB.Difficulties()
                .getList());
            difficulties.sort((a, b) -> Integer.compare(a.rank, b.rank));
        } catch (Exception e) {
            e.printStackTrace();
        }

        RPGPlayerData rpg = playerData();
        if (rpg != null) {
            DungeonData current = rpg.maps.dungeonData;
            if (current != null && !current.diff.isEmpty() && isRegistered(current.diff)) {
                selectedDiff = current.diff;
            }
            if (current != null && current.team != null) {
                selectedTeam = current.team;
            }
        }

        if (selectedDiff.isEmpty() || !isUnlocked(difficulty(selectedDiff))) {
            selectedDiff = defaultDifficulty();
        }
    }

    // ------------------------------------------------------------------ данные

    private RPGPlayerData playerData() {
        try {
            if (mc.player == null) {
                return null;
            }
            return Load.playerRPGData(mc.player);
        } catch (Exception e) {
            return null;
        }
    }

    private int playerLevel() {
        try {
            return Load.Unit(mc.player)
                .getLevel();
        } catch (Exception e) {
            return 1;
        }
    }

    private boolean isRegistered(String id) {
        return id != null && !id.isEmpty() && ExileDB.Difficulties()
            .isRegistered(id);
    }

    private Difficulty difficulty(String id) {
        if (!isRegistered(id)) {
            return null;
        }
        return ExileDB.Difficulties()
            .get(id);
    }

    private Difficulty selected() {
        return difficulty(selectedDiff);
    }

    private boolean isUnlocked(Difficulty diff) {
        return diff != null && playerLevel() >= diff.req_player_lvl;
    }

    /** Сложность по умолчанию: самая высокая из доступных, иначе первая. */
    private String defaultDifficulty() {
        String best = "";
        for (Difficulty d : difficulties) {
            if (isUnlocked(d)) {
                if (best.isEmpty() || d.rank > difficulty(best).rank) {
                    best = d.id;
                }
            }
        }
        if (best.isEmpty() && !difficulties.isEmpty()) {
            best = difficulties.get(0).id;
        }
        return best;
    }

    private static ChatFormatting colorOf(Difficulty diff) {
        if (diff == null) {
            return ChatFormatting.GRAY;
        }
        switch (diff.rank) {
            case 0:
                return ChatFormatting.GREEN;
            case 1:
                return ChatFormatting.GOLD;
            case 2:
                return ChatFormatting.RED;
            default:
                return ChatFormatting.AQUA;
        }
    }

    private String diffName(Difficulty diff) {
        if (diff == null) {
            return "—";
        }
        try {
            return CLOC.translate(diff.locName());
        } catch (Exception e) {
            return diff.id;
        }
    }

    // ------------------------------------------------------------------ кнопки

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.children.clear();

        if (!difficulties.isEmpty()) {
            int shown = Math.min(difficulties.size(), 4);
            for (int i = 0; i < shown; i++) {
                final Difficulty diff = difficulties.get(i);
                boolean unlocked = isUnlocked(diff);
                boolean isSelected = diff.id.equals(selectedDiff);

                String prefix = isSelected ? "§e> " : (unlocked ? "§7  " : "§8✖ ");
                String label = prefix + "§" + colorCode(colorOf(diff)) + diffName(diff)
                    + " §7(ур. " + diff.req_player_lvl + "+)";

                addButton(new PortalButton(
                    guiLeft + LEFT_X, guiTop + TOP_Y + i * 24, LEFT_W, 20,
                    clip(label, LEFT_W - 6),
                    unlocked,
                    b -> {
                        selectedDiff = diff.id;
                        this.init();
                    },
                    difficultyTooltip(diff)
                ));
            }
        }

        // выбор отряда
        int teamY = guiTop + TOP_Y + 4 * 24 + 4;

        int bx = guiLeft + LEFT_X;
        for (TeamSize size : new TeamSize[]{TeamSize.SOLO, TeamSize.DUO, TeamSize.TEAM}) {
            boolean isSelected = size == selectedTeam;
            String label = (isSelected ? "§e> " : "§7") + CLOC.translate(size.word.locName());
            addButton(new PortalButton(
                bx, teamY, 40, 20,
                clip(label, 38),
                true,
                b -> {
                    selectedTeam = size;
                    this.init();
                },
                teamTooltip(size)
            ));
            bx += 43;
        }

        Difficulty diff = selected();
        boolean canStart = diff != null && isUnlocked(diff);

        addButton(new PortalButton(
            guiLeft + RIGHT_X, guiTop + BAR_Y, RIGHT_W, 22,
            canStart ? "§aНачать экспедицию" : "§8Начать экспедицию",
            canStart,
            b -> start(),
            startTooltip(diff)
        ));

        addButton(new PortalButton(
            guiLeft + LEFT_X, guiTop + BAR_Y, LEFT_W, 22,
            "§7Закрыть",
            true,
            b -> this.onClose(),
            List.of(Component.literal("Вернуться в мир").withStyle(ChatFormatting.GRAY))
        ));
    }

    private void start() {
        Difficulty diff = selected();
        if (diff == null || !isUnlocked(diff)) {
            return;
        }
        Packets.sendToServer(new StartDungeonPacket(selectedTeam, teleporterPos, diff.id));
        this.onClose();
    }

    // ------------------------------------------------------------------ тултипы

    private List<Component> difficultyTooltip(Difficulty diff) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(diffName(diff)).withStyle(colorOf(diff), ChatFormatting.BOLD));

        try {
            list.addAll(diff.getTooltip());
        } catch (Exception e) {
            list.add(Component.literal("§7Здоровье/урон мобов: " + (int) (diff.hp_multi * 100) + "%").withStyle(ChatFormatting.GRAY));
        }

        list.add(Component.literal(""));
        list.add(Component.literal("§7Смертей до провала: §f" + (int) (diff.deaths_allowed * selectedTeam.deathsAllowedMulti)));
        list.add(Component.literal("§7Штраф благосклонности за смерть: §c" + diff.death_favor_penalty));

        if (!isUnlocked(diff)) {
            list.add(Component.literal(""));
            list.add(Component.literal("§cТребуется уровень персонажа: " + diff.req_player_lvl)
                .withStyle(ChatFormatting.RED));
        }
        return list;
    }

    private List<Component> teamTooltip(TeamSize size) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(CLOC.translate(size.word.locName())).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        list.add(Component.literal("§7Нужно игроков: §f" + size.requiredMemberAmount + "§7–§f" + size.maxMembers));
        list.add(Component.literal("§7Добыча: §6×" + size.lootMulti));
        list.add(Component.literal("§7Опыт: §b×" + size.expMulti));
        list.add(Component.literal("§7Лимит смертей: §f×" + size.deathsAllowedMulti));

        TeamSizeMobStrength str = size.getMobStr(DungeonType.DUNGEON);
        list.add(Component.literal("§7Мобы: §c+" + str.hp + "% HP, +" + str.dmg + " урона"));

        if (size.requiredMemberAmount > 1) {
            list.add(Component.literal("§8Нужна группа (/age_of_exile teams)").withStyle(ChatFormatting.DARK_GRAY));
        }
        return list;
    }

    private List<Component> startTooltip(Difficulty diff) {
        List<Component> list = new ArrayList<>();
        if (diff == null) {
            list.add(Component.literal("§cСложности ещё не загрузились").withStyle(ChatFormatting.RED));
            return list;
        }
        if (!isUnlocked(diff)) {
            list.add(Component.literal("§cНужен уровень " + diff.req_player_lvl + ", у вас " + playerLevel())
                .withStyle(ChatFormatting.RED));
            return list;
        }
        list.add(Component.literal("§aСоздать новое подземелье").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        list.add(Component.literal("§7Сложность: §f" + diffName(diff)));
        list.add(Component.literal("§7Отряд: §f" + CLOC.translate(selectedTeam.word.locName())));
        list.add(Component.literal("§7Уровень подземелья: §f" + playerLevel()));
        list.add(Component.literal(""));
        list.add(Component.literal("§8Портал останется в мире, вернуться можно").withStyle(ChatFormatting.DARK_GRAY));
        list.add(Component.literal("§8через обратный портал внутри подземелья.").withStyle(ChatFormatting.DARK_GRAY));
        return list;
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.bindTexture(PANEL);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        AbstractGui.blit(matrix, guiLeft, guiTop, SIZE_X, SIZE_Y, 0F, 0F, SIZE_X, SIZE_Y, TEX_W, TEX_H);

        AbstractGui.drawShadow(mc.font, matrix, "§cПОРТАЛ В ПОДЗЕМЕЛЬЕ",
            guiLeft + LEFT_X, guiTop + 10, 0xFFFFFF);
        AbstractGui.drawShadow(mc.font, matrix, "§7Уровень персонажа: §f" + playerLevel()
                + "§7 · выберите сложность и отряд",
            guiLeft + LEFT_X, guiTop + 21, 0xFFFFFF);

        // разделитель колонок
        int dividerX = guiLeft + RIGHT_X - 6;
        AbstractGui.fill(matrix, dividerX, guiTop + TOP_Y - 4, dividerX + 1,
            guiTop + BAR_Y - 8, 0x40FFFFFF);

        renderTeamHeader(matrix);
        renderInfo(matrix);

        super.render(matrix, mouseX, mouseY, partialTicks);
        buttons.forEach(b -> b.renderToolTip(matrix, mouseX, mouseY));
    }

    private void renderTeamHeader(PoseStack matrix) {
        int y = guiTop + TOP_Y + 4 * 24 - 6;
        AbstractGui.fill(matrix, guiLeft + LEFT_X, y + 9, guiLeft + LEFT_X + LEFT_W, y + 10, 0x40FFFFFF);
        AbstractGui.drawShadow(mc.font, matrix, "§8Отряд", guiLeft + LEFT_X, y, 0xFFFFFF);
    }

    private void renderInfo(PoseStack matrix) {
        int x = guiLeft + RIGHT_X;
        int y = guiTop + TOP_Y;
        int bottom = guiTop + BAR_Y - 8;

        Difficulty diff = selected();
        if (diff == null) {
            AbstractGui.drawShadow(mc.font, matrix, "§7Нет данных о сложности",
                x, y, 0xFFFFFF);
            return;
        }

        AbstractGui.drawShadow(mc.font, matrix,
            "§l" + clip(diffName(diff), RIGHT_W), x, y, 0xFFFFFF);
        y += LINE + 2;

        AbstractGui.drawShadow(mc.font, matrix,
            "§7Уровень подземелья: §f" + Math.max(1, playerLevel()), x, y, 0xFFFFFF);
        y += LINE;

        y = stat(matrix, "Здоровье мобов", (int) (diff.hp_multi * 100) + "%", x, y);
        y = stat(matrix, "Урон мобов", (int) (diff.dmg_multi * 100) + "%", x, y);
        y = stat(matrix, "Характеристики", (int) (diff.stat_multi * 100) + "%", x, y);
        y = stat(matrix, "Добыча", "×" + diff.loot_multi, x, y);
        y += 4;

        AbstractGui.fill(matrix, guiLeft + RIGHT_X, y - 2, guiLeft + RIGHT_X + RIGHT_W, y - 1, 0x30FFFFFF);
        y += 4;

        TeamSizeMobStrength str = selectedTeam.getMobStr(DungeonType.DUNGEON);
        y = stat(matrix, "Отряд", CLOC.translate(selectedTeam.word.locName()), x, y);
        y = stat(matrix, "Добыча отряда", "×" + selectedTeam.lootMulti, x, y);
        y = stat(matrix, "Опыт отряда", "×" + selectedTeam.expMulti, x, y);
        y = stat(matrix, "Смертей до провала",
            String.valueOf((int) (diff.deaths_allowed * selectedTeam.deathsAllowedMulti)), x, y);
        y = stat(matrix, "Сила мобов", "+" + str.hp + "% HP", x, y);

        if (y + LINE < bottom) {
            y += 4;
            if (isUnlocked(diff)) {
                AbstractGui.drawShadow(mc.font, matrix, "§a✔ Сложность доступна", x, y, 0xFFFFFF);
            } else {
                AbstractGui.drawShadow(mc.font, matrix,
                    clip("§cНужен уровень " + diff.req_player_lvl, RIGHT_W), x, y, 0xFFFFFF);
            }
        }
    }

    private int stat(PoseStack matrix, String label, String value, int x, int y) {
        AbstractGui.drawShadow(mc.font, matrix, "§7" + clip(label, 74) + ": §f" + value, x, y, 0xFFFFFF);
        return y + LINE;
    }

    // ------------------------------------------------------------------ утилиты

    private static String colorCode(ChatFormatting format) {
        return String.valueOf(com.robertx22.forge_compat.common.CompatChatFormatting.getCode(format));
    }

    private String clip(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (mc.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (mc.font.width(result.toString() + c + ellipsis) > maxWidth) {
                break;
            }
            result.append(c);
        }
        return result + ellipsis;
    }

    private static class PortalButton extends Button {
        private final List<Component> tooltip;

        PortalButton(int x, int y, int w, int h, String label, boolean active,
                     net.minecraft.client.gui.components.Button.OnPress onPress,
                     List<Component> tooltip) {
            super(x, y, w, h, Component.literal(label), onPress);
            this.active = active;
            this.tooltip = tooltip != null ? tooltip : List.of();
        }

        @Override
        public void renderToolTip(PoseStack matrix, int mouseX, int mouseY) {
            if (isHovered() && !tooltip.isEmpty()) {
                GuiUtils.renderTooltip(matrix, tooltip, mouseX, mouseY);
            }
        }
    }
}
