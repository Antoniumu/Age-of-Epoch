package com.robertx22.age_of_exile.saveclasses.item_classes;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.data.affixes.Affix;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.gear_types.bases.SlotFamily;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.requirements.bases.GearRequestedFor;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.saveclasses.ExactStatData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.*;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.*;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.ICommonDataItem;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.library_of_exile.utils.ItemstackDataSaver;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Storable
public class GearItemData implements ICommonDataItem<GearRarity> {

    // Stats
    @Store
    public BaseStatsData baseStats = new BaseStatsData();
    @Store
    public ImplicitStatsData imp = new ImplicitStatsData(); // implicit stats
    @Store
    public GearAffixesData affixes = new GearAffixesData();
    @Store
    public GearSocketsData sockets = new GearSocketsData();
    @Store
    public CraftedStatsData cr;
    @Store
    public UniqueStatsData uniqueStats;
    @Store
    public UpgradeData up = new UpgradeData();

    @Store
    public String spell = "";

    // Stats

    // i added rename ideas to comments. As tiny as possible while still allowing people to understand kinda what it is
    // apparently people had big issues with many storage mods, So i should try minimize the nbt.
    @Store
    public String uniq_id = ""; // uniq_id

    @Store
    public String rarity = IRarity.COMMON_ID; // rar

    @Store
    public String item_id = ""; // item registry name

    @Store
    public int rp = -1; // pre_name rare prefix
    @Store
    public int rs = -1; // suf_name rare suffix

    @Store
    public int lvl = 1; // lvl

    @Store
    public String gear_type = "";

    @Store
    private float in = 0;

    @Store
    public boolean can_sal = true;

    @Store
    public boolean c = false; // corrupted

    /**
     * «Бесдушная основа»: у предмета есть только слабые базовые статы и common-редкость,
     * всё остальное (аффиксы, имя, уникат, сокеты) приносит настоящая душа.
     * См. {@link com.robertx22.age_of_exile.saveclasses.stat_soul.SoullessGear}.
     */
    @Store
    public boolean sl = false;

    /**
     * Множитель базовых статов (1 — обычный предмет). У бесдушной основы это
     * «сила основы»: 0.12 у дешёвого предмета … 0.45 у самого дорогого.
     * Хранится отдельно от роллов {@code baseStats.perc}, потому что perc лишь
     * выбирает значение внутри диапазона min..max, а не масштабирует его.
     */
    @Store
    public float bq = 1F;

    public boolean isSoulless() {
        return sl;
    }

    public void upgradeToHigherRarity() {

        GearRarity old = this.getRarity();
        if (old == null) {
            return;
        }
        GearRarity rar = old.getHigherRarity();
        if (rar == null) {
            // Финальная редкость (epic/unique/runeword) вверх не растёт.
            return;
        }

        this.rarity = rar.GUID();

        int affixes = rar.affixes - old.affixes;

        for (int i = 0; i < affixes; i++) {
            this.affixes.addOneRandomAffix(this);
        }

        // Не regenerate() - иначе игрок терял бы все вложенные камни улучшения
        // ровно в момент, когда они понадобились для повышения редкости.
        // Досоздаём/убираем только пустые слоты под новый потолок редкости.
        while (this.up.ups.size() < rar.upgrades) {
            this.up.ups.add(UpgradeData.SlotType.EMPTY);
        }
        while (this.up.ups.size() > rar.upgrades) {
            int lastEmpty = -1;
            for (int i = this.up.ups.size() - 1; i >= 0; i--) {
                if (this.up.ups.get(i).isEmptySlot) {
                    lastEmpty = i;
                    break;
                }
            }
            if (lastEmpty == -1) {
                break;
            }
            this.up.ups.remove(lastEmpty);
        }
    }

    public void onUpgrade(Player player, UpgradeData.SlotType type) {

        for (int i = 0; i < this.up.ups.size(); i++) {
            UpgradeData.SlotType slot = this.up.ups.get(i);
            if (slot == UpgradeData.SlotType.EMPTY && type == UpgradeData.SlotType.UP1) {
                this.up.ups.set(i, type);
                break;
            } else if (slot == UpgradeData.SlotType.UP1 && type == UpgradeData.SlotType.UP2) {
                this.up.ups.set(i, type);
                break;
            } else if (slot == UpgradeData.SlotType.UP2 && type == UpgradeData.SlotType.UP3) {
                this.up.ups.set(i, type);
                break;
            }
        }

        if (type.upgradeLevel > 0) {
            player.sendSystemMessage(Component.literal("Предмет улучшен до +" + up.getUpgradeLevel() + " (+" + type.upgradeLevel + ")").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("Улучшение не удалось").withStyle(ChatFormatting.RED));
        }

    }

    public boolean isCorrupted() {
        return c;
    }

    public boolean hasCraftedStats() {
        return cr != null;
    }

    public CraftedStatsData getCraftedStats() {
        return cr;
    }

    public int getTier() {
        return LevelUtils.levelToTier(lvl);
    }

    public float getILVL() {

        float ilvl = lvl + getRarity().bonus_effective_lvls;

        int upgrades = (int) (ServerContainer.get().ILVL_PER_UPGRADE_LEVEL.get() * this.up.getUpgradeLevel());
        if (upgrades > 0) {
            ilvl += upgrades;
        }
        return ilvl;
    }

    public boolean canPlayerWear(EntityData data) {

        if (LevelUtils.tierToLevel(getTier()) > data.getLevel()) {
            return false;
        }

        return true;

    }

    public boolean isValidItem() {

        return ExileDB.GearTypes()
            .isRegistered(gear_type);
    }

    public boolean hasSpell() {
        return ExileDB.Spells()
            .isRegistered(spell);

    }

    public Spell getSpell() {
        return ExileDB.Spells()
            .get(spell);
    }

    public int getTotalSockets() {
        int sockets = getRarity().sockets;
        int upgradeLevel = this.up.getUpgradeLevel();

        // Апгрейды имеют три ступени на слот (UP1/UP2/UP3), поэтому
        // uncommon (2 слота), rare (3) и epic/unique (5) могут достичь +4/+8/+12.
        if (upgradeLevel >= 4) {
            sockets++;
        }
        if (upgradeLevel >= 8) {
            sockets++;
        }
        if (upgradeLevel >= 12) {
            sockets++;
        }
        return sockets;
    }

    public int getEmptySockets() {

        return getTotalSockets() - this.sockets.getSocketedGemsCount();
    }

    public boolean canGetAffix(Affix affix) {

        if (affix.only_one_per_item && affixes.containsAffix(affix)) {
            return false;
        }

        return affix.meetsRequirements(new GearRequestedFor(this));

    }

    public float getInstability() {
        return in;
    }

    public void setInstability(float insta) {
        this.in = insta;

        if (in < 0) {
            in = 0;
        }
    }

    public GearItemEnum getGearEnum() {

        if (this.isUnique()) {
            return GearItemEnum.UNIQUE;
        }

        return GearItemEnum.NORMAL;
    }

    @Override
    public String getRarityRank() {
        return rarity;
    }

    @Override
    public GearRarity getRarity() {
        return ExileDB.GearRarities()
            .get(this.rarity);
    }

    public MutableComponent name(ItemStack stack) {
        return stack.getHoverName().copy();
    }

    public void WriteOverDataThatShouldStay(GearItemData newdata) {

        newdata.can_sal = this.can_sal;

    }

    public BaseGearType GetBaseGearType() {
        return ExileDB.GearTypes()
            .get(gear_type);
    }

    public List<MutableComponent> GetDisplayName(ItemStack stack) {

        try {
            if (this.isSoulless()) {
                // Бесдушная основа не имеет ни редкости, ни имени: это просто предмет.
                MutableComponent plain = stack.getHoverName()
                    .copy()
                    .withStyle(ChatFormatting.GRAY);
                return TooltipUtils.cutIfTooLong(plain, ChatFormatting.GRAY);
            }

            ChatFormatting format = this.getRarity()
                .textFormatting();

            if (useFullAffixedName()) {
                return getFullAffixedName();
            } else {
                if (isUnique()) {
                    return getUniqueName();
                } else {
                    return getTooManyAffixesName();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList();
    }

    public boolean uniqueBaseStatsReplaceBaseStats() {
        return uniqueStats != null && this.uniqueStats.getUnique(this) != null && !uniqueStats.getUnique(this).base_stats.isEmpty();
    }

    private List<MutableComponent> getFullAffixedName() {
        List<MutableComponent> list = new ArrayList<>();
        ChatFormatting format = this.getRarity()
            .textFormatting();

        MutableComponent text = Component.literal("");

        if (affixes.hasPrefix()) {

            AffixData prefix = affixes.pre.get(0);

            text
                .append(prefix.BaseAffix()
                    .locName()
                    .append(" "));
        }
        if (this.uniqueStats == null) {
            text.append(GetBaseGearType().locName());
        } else {
            text.append(uniqueStats.getUnique(this)
                .locName()
            );
        }

        if (affixes.hasSuffix()) {
            AffixData suffix = affixes.suf.get(0);
            text.append(" ")
                .append(suffix.BaseAffix()
                    .locName());
        }

        text.withStyle(format);

        list.addAll(TooltipUtils.cutIfTooLong(text, format));

        return list;

    }

    private List<MutableComponent> getUniqueName() {
        List<MutableComponent> list = new ArrayList<>();
        ChatFormatting format = this.getRarity()
            .textFormatting();

        UniqueGear uniq = this.uniqueStats.getUnique(this);

        MutableComponent txt = Component.literal("").append(uniq.locName()
            .withStyle(format));

        if (!uniq.replaces_name) {
            txt.append(Component.literal(format + " ").append(GetBaseGearType().locName()
                .withStyle(format)));
        }

        list.addAll(TooltipUtils.cutIfTooLong(txt, format));

        return list;
    }

    private List<MutableComponent> getTooManyAffixesName() {
        List<MutableComponent> list = new ArrayList<>();
        ChatFormatting format = this.getRarity()
            .textFormatting();

        Words prefix = RareItemAffixNames.getPrefix(this);
        Words suffix = RareItemAffixNames.getSuffix(this);

        if (prefix != null && suffix != null) {

            MutableComponent txt = Component.literal("");

            txt.append(Component.literal("").append(prefix.locName())
                .append(" "));

            txt.append(Component.literal("").append(suffix.locName())
                .withStyle(format));

            txt.append(Component.literal(" ").append(GetBaseGearType().locName()));

            txt.withStyle(format);

            list.addAll(TooltipUtils.cutIfTooLong(txt, format));

            return list;
        }

        return list;

    }

    private boolean useFullAffixedName() {

        if (isUnique() && affixes.getNumberOfAffixes() == 0) {
            return false;
        }
        if (affixes.getNumberOfPrefixes() > 1) {
            return false;
        }
        if (affixes.getNumberOfSuffixes() > 1) {
            return false;
        }
        return true;
    }

    public List<IStatsContainer> GetAllStatContainers() {

        List<IStatsContainer> list = new ArrayList<IStatsContainer>();

        IfNotNullAdd(baseStats, list);

        IfNotNullAdd(cr, list);

        IfNotNullAdd(imp, list);

        affixes.getAllAffixesAndSockets()
            .forEach(x -> IfNotNullAdd(x, list));

        IfNotNullAdd(sockets, list);

        IfNotNullAdd(uniqueStats, list);

        return list;

    }

    public List<ExactStatData> GetAllStats() {

        List<ExactStatData> list = new ArrayList<>();
        for (IStatsContainer x : GetAllStatContainers()) {

            List<ExactStatData> stats = x.GetAllStats(this);

            stats.forEach(s -> {
                list.add(s);
            });

        }
        return list;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void BuildTooltip(TooltipContext ctx) {
        GearTooltipUtils.BuildTooltip(this, ctx.stack, ctx.tooltip, ctx.data);
    }

    public List<IRerollable> GetAllRerollable() {
        List<IRerollable> list = new ArrayList<IRerollable>();
        IfNotNullAdd(baseStats, list);

        affixes.getAllAffixesAndSockets()
            .forEach(x -> IfNotNullAdd(x, list));

        list.add(imp);

        IfNotNullAdd(uniqueStats, list);
        return list;
    }

    private <T> void IfNotNullAdd(T obj, List<T> list) {
        if (obj != null) {
            list.add(obj);
        }
    }

    public static class SalvagedItemInfo {

        public int tier;
        public GearRarity rarity;

        public SalvagedItemInfo(int tier, GearRarity rarity) {
            this.tier = tier;
            this.rarity = rarity;
        }
    }

    public static List<ItemStack> getSalvagedResults(SalvagedItemInfo info) {
        try {
            List<ItemStack> list = new ArrayList<>();

            ItemStack dust = new ItemStack(ProfessionItems.SALVAGED_ESSENCE_MAP.get(SkillItemTier.of(info.tier))
                .get());

            dust.setCount(info.rarity.dust_per_sal.random());

            list.add(dust);

            if (info.rarity.rar_ess_per_sal > 0) {
                ItemStack essence = info.rarity.getRarityEssenceStack();
                essence.setCount(info.rarity.rar_ess_per_sal);
                list.add(essence);
            }

            return list;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList(ItemStack.EMPTY);
    }

    @Override
    public List<ItemStack> getSalvageResult(ItemStack stack) {
        if (this.can_sal) {
            return getSalvagedResults(new SalvagedItemInfo(getTier(), getRarity()));
        }

        return Arrays.asList(ItemStack.EMPTY);
    }

    @Override
    public boolean isSalvagable(SalvageContext context) {
        return this.can_sal;
    }

    @Override
    public ItemstackDataSaver<GearItemData> getStackSaver() {
        return StackSaving.GEARS;
    }

    @Override
    public void saveToStack(ItemStack stack) {
        getStackSaver().saveTo(stack, this);
    }

    public boolean isWeapon() {
        try {
            if (GetBaseGearType()
                .family()
                .equals(SlotFamily.Weapon)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isBetterThan(GearItemData other) {

        if (other.lvl > lvl + 10) {
            return false;
        }
        return getRarity()
            .isHigherThan(other.getRarity());
    }

}
