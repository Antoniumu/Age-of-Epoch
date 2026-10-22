package com.robertx22.age_of_exile.database.data.perks;

import com.robertx22.age_of_exile.database.OptScaleExactStat;
import com.robertx22.age_of_exile.database.data.stats.Stat;
import com.robertx22.age_of_exile.database.data.stats.tooltips.StatTooltipType;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.ITooltipList;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ClientTextureUtils;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.registry.serialization.IByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;


import net.minecraft.network.Connection;
public class Perk implements JsonExileRegistry<Perk>, IAutoGson<Perk>, ITooltipList, IByteBuf<Perk>, IAutoLocName {
    public static Perk SERIALIZER = new Perk();

    public PerkType type;
    public String identifier;
    public String icon = "";
    public String one_of_a_kind = "";

    /**
     * Взаимоисключающая ветка древа. Если у игрока уже изучен талант из другой ветки (другое
     * непустое значение этого поля), этот талант взять нельзя. Нужно для древа «Божественный»:
     * некромант и призыватель не выбираются одновременно.
     */
    public String exclusive_branch = "";

    public transient String locname = "";

    public boolean is_entry = false;
    public int lvl_req = 1;

    public List<OptScaleExactStat> stats = new ArrayList<>();

    @Override
    public Perk getFromBuf(FriendlyByteBuf buf) {
        Perk data = new Perk();

        data.type = PerkType.valueOf(buf.readUtf(50));
        data.identifier = buf.readUtf(100);
        data.icon = buf.readUtf(150);
        data.one_of_a_kind = buf.readUtf(80);
        if (data.one_of_a_kind.isEmpty()) {
            data.one_of_a_kind = null;
        }

        data.exclusive_branch = buf.readUtf(80);
        if (data.exclusive_branch.isEmpty()) {
            data.exclusive_branch = null;
        }

        data.is_entry = buf.readBoolean();
        data.lvl_req = buf.readInt();

        int s = buf.readInt();

        for (int i = 0; i < s; i++) {
            data.stats.add(OptScaleExactStat.SERIALIZER.getFromBuf(buf));
        }

        return data;
    }

    @Override
    public void toBuf(FriendlyByteBuf buf) {
        buf.writeUtf(type != null ? type.name() : PerkType.STAT.name(), 100);
        buf.writeUtf(identifier != null ? identifier : "", 100);
        buf.writeUtf(icon != null ? icon : "", 150);

        buf.writeUtf(one_of_a_kind != null ? one_of_a_kind : "", 80);

        buf.writeUtf(exclusive_branch != null ? exclusive_branch : "", 80);

        buf.writeBoolean(is_entry);
        buf.writeInt(lvl_req);

        buf.writeInt(stats.size());
        stats.forEach(x -> x.toBuf(buf));

    }

    transient Identifier cachedIcon = null;

    public Identifier getIcon() {
        if (cachedIcon == null) {
            Identifier id = Identifier.parse(icon);
            if (ClientTextureUtils.textureExists(id)) {
                cachedIcon = id;
            } else {
                cachedIcon = Stat.DEFAULT_ICON;
            }
        }
        return cachedIcon;
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public List<Component> GetTooltipString(TooltipInfo info) {
        List<Component> list = new ArrayList<>();

        try {

            if (this.type != PerkType.STAT) {
                list.add(locName().withStyle(type.format));
            }

            info.statTooltipType = StatTooltipType.BASE_LOCAL_STATS;

            stats.forEach(x -> list.addAll(x.GetTooltipString(info)));


            if (this.one_of_a_kind != null && !this.one_of_a_kind.isEmpty()) {
                list.add(Component.literal("Можно выбрать только один талант этого типа: ").withStyle(ChatFormatting.GREEN));

                list.add(Component.translatable(SlashRef.MODID + ".one_of_a_kind." + one_of_a_kind).withStyle(ChatFormatting.GREEN));
            }

            if (this.exclusive_branch != null && !this.exclusive_branch.isEmpty()) {
                list.add(Component.literal("Взаимоисключающая ветка: ")
                    .append(Component.translatable(SlashRef.MODID + ".branch." + exclusive_branch.replace('/', '.')))
                    .withStyle(ChatFormatting.RED));

                list.add(Component.literal("Взять таланты другой ветки призыва не получится, пока эта не сброшена.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            }

            if (lvl_req > 1) {
                list.add(Words.RequiresLevel.locName()
                        .append(": " + lvl_req)
                        .withStyle(ChatFormatting.YELLOW));
            }

            if (this.type == PerkType.MAJOR) {

                list.add(Component.literal("Ключевой талант.").withStyle(ChatFormatting.RED));
            }

            if (info.shouldShowDescriptions()) {
                if (this.type == PerkType.START) {
                    list.add(Component.literal(" [Стартовый узел древа талантов. Выберите один из стартовых путей, чтобы открыть соседние узлы.]")
                        .withStyle(ChatFormatting.BLUE));
                }
            } else {
                list.add(Words.PressAltForStatInfo.locName()
                        .withStyle(ChatFormatting.BLUE));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Talents;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".talent." + identifier;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    public enum Connection {
        LINKED, BLOCKED, POSSIBLE
    }

    public enum PerkType {
        STAT(2, 24, 24, 39, ChatFormatting.WHITE),
        SPECIAL(3, 28, 28, 77, ChatFormatting.LIGHT_PURPLE),
        MAJOR(1, 33, 33, 1, ChatFormatting.RED),
        START(4, 35, 35, 115, ChatFormatting.YELLOW),
        SPELL_MOD(5, 26, 26, 153, ChatFormatting.BLACK);

        int order;

        public int width;
        public int height;
        private int xoff;
        public ChatFormatting format;

        PerkType(int order, int width, int height, int xoff, ChatFormatting format) {
            this.order = order;
            this.width = width;
            this.height = height;
            this.xoff = xoff;
            this.format = format;
        }

        public int getXOffset() {
            return xoff;
        }

    }


    public PerkType getType() {
        return type;
    }

    /** Класс ветки — часть до «/»: в «divine/necromancer» это «divine». */
    public static String branchClass(String branch) {
        if (branch == null) {
            return "";
        }
        int i = branch.indexOf('/');
        return i < 0 ? branch : branch.substring(0, i);
    }

    /** Путь ветки — часть после «/»; пусто, если узел общий для всего класса. */
    public static String branchPath(String branch) {
        if (branch == null) {
            return "";
        }
        int i = branch.indexOf('/');
        return i < 0 ? "" : branch.substring(i + 1);
    }

    /**
     * Конфликтуют ли две ветки. Разные классы взаимоисключают друг друга полностью;
     * внутри одного класса конфликтуют только разные пути (общие узлы класса конфликта не дают).
     */
    public static boolean branchesConflict(String mine, String taken) {
        if (mine == null || mine.isEmpty() || taken == null || taken.isEmpty()) {
            return false;
        }
        if (!branchClass(mine).equals(branchClass(taken))) {
            return true;
        }
        String a = branchPath(mine);
        String b = branchPath(taken);
        return !a.isEmpty() && !b.isEmpty() && !a.equals(b);
    }

    public boolean isLockedToPlayer(Player player) {

        if (Load.Unit(player)
                .getLevel() < lvl_req) {
            return true;
        }

        if (one_of_a_kind != null) {
            if (!one_of_a_kind.isEmpty()) {

                if (Load.playerRPGData(player).talents
                        .getAllAllocatedPerks()
                        .values()
                        .stream()
                        .filter(x -> {
                            return x != null && this.one_of_a_kind.equals(x.one_of_a_kind);
                        })
                        .count() > 0) {
                    return true;
                }
            }
        }


        if (exclusive_branch != null && !exclusive_branch.isEmpty()) {
            boolean conflict = Load.playerRPGData(player).talents.getAllAllocatedPerks()
                .values()
                .stream()
                .anyMatch(x -> x != null && branchesConflict(this.exclusive_branch, x.exclusive_branch));
            if (conflict) {
                return true; // уже взята конфликтующая ветка: другой класс или другой путь того же класса
            }
        }

        return false;

    }

    @Override
    public Class<Perk> getClassForSerialization() {
        return Perk.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.PERK;
    }

    @Override
    public String GUID() {
        return identifier;
    }
}
