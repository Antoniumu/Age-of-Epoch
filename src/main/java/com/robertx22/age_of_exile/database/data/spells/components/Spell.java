package com.robertx22.age_of_exile.database.data.spells.components;

import com.google.gson.Gson;
import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.aoe_data.database.spells.SpellDesc;
import com.robertx22.age_of_exile.database.data.StatModifier;
import com.robertx22.age_of_exile.database.data.exile_effects.ExileEffect;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.spells.SpellCastType;
import com.robertx22.age_of_exile.database.data.spells.SpellTag;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.age_of_exile.database.data.value_calc.MaxLevelProvider;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.effectdatas.SpendResourceEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.rework.EventData;
import com.robertx22.age_of_exile.uncommon.enumclasses.AttackType;
import com.robertx22.age_of_exile.uncommon.enumclasses.WeaponTypes;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocDesc;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.age_of_exile.uncommon.utilityclasses.MapManager;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.IGUID;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Spell implements IGUID, IAutoGson<Spell>, JsonExileRegistry<Spell>, IAutoLocName, IAutoLocDesc, MaxLevelProvider {
    public static Spell SERIALIZER = new Spell();

    public static String DEFAULT_EN_NAME = "default_entity_name";
    public static String CASTER_NAME = "caster";

    public static Gson GSON = new Gson();

    public int max_lvl = 16;
    public int weight = 1000;
    public String identifier = "";
    public AttachedSpell attached = new AttachedSpell();
    public SpellConfiguration config = new SpellConfiguration();

    public boolean manual_tip = false;
    public List<String> disabled_dims = new ArrayList<>();
    public String effect_tip = "";

    public transient String locDesc = "";
    public transient List<StatModifier> statsForSkillGem = new ArrayList<>();

    public boolean isAllowedInDimension(Level world) {
        if (disabled_dims.isEmpty()) {
            return true;
        }
        return disabled_dims.stream()
            .map(x -> Identifier.parse(x))
            .noneMatch(x -> x.equals(MapManager.getResourceLocation(world)));

    }

    public AttachedSpell getAttached() {
        return attached;
    }

    public boolean is(SpellTag tag) {
        return config.tags.contains(tag);
    }

    public SpellConfiguration getConfig() {
        return config;
    }

    public void validate() {
        for (ComponentPart x : this.attached.getAllComponents()) {
            x.validate();
        }
    }

    public final Identifier getIconLoc() {
        return getIconLoc(GUID());
    }

    public static final Identifier getIconLoc(String id) {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/icons/" + id + ".png");
    }

    public WeaponTypes getWeapon(LivingEntity en) {
        try {
            if (getConfig().style.getAttackType() != AttackType.spell) {

                ItemStack stack = en.getMainHandItem();

                GearItemData gear = Gear.Load(stack);

                if (gear != null) {
                    return gear.GetBaseGearType().weapon_type;
                }

                if (!stack.isEmpty()) {
                    GearSlot slot = GearSlot.getSlotOf(stack.getItem());
                    if (slot != null) {
                        String id = slot.GUID();
                        if (GearSlots.BOW.equals(id)) {
                            return WeaponTypes.bow;
                        }
                        if (GearSlots.CROSBOW.equals(id)) {
                            return WeaponTypes.crossbow;
                        }
                        if (GearSlots.SWORD.equals(id)) {
                            return WeaponTypes.sword;
                        }
                        if (GearSlots.AXE.equals(id)) {
                            return WeaponTypes.axe;
                        }
                        if (GearSlots.STAFF.equals(id)) {
                            return WeaponTypes.staff;
                        }
                        if (GearSlots.SCEPTER.equals(id)) {
                            return WeaponTypes.scepter;
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return WeaponTypes.none;
    }

    public final void onCastingTick(SpellCastContext ctx) {

        int timesToCast = (int) ctx.spell.getConfig().times_to_cast;

        if (timesToCast > 1) {

            int castTimeTicks = (int) getCastTimeTicks(ctx);

            // if i didnt do this then cast time reduction would reduce amount of spell hits.
            int castEveryXTicks = castTimeTicks / timesToCast;

            if (timesToCast > 1) {
                if (castEveryXTicks < 1) {
                    castEveryXTicks = 1;
                }
            }

            if (ctx.isLastCastTick) {
                this.cast(ctx, true);
            } else {
                if (ctx.ticksInUse > 0 && ctx.ticksInUse % castEveryXTicks == 0) {
                    this.cast(ctx, true);
                }
            }

        } else if (timesToCast < 1) {
            System.out.println("Times to cast spell is: " + timesToCast + " . this seems like a bug.");
        }

    }

    public void cast(SpellCastContext ctx, boolean imbue) {

        LivingEntity caster = ctx.caster;

        ctx.castedThisTick = true;

        if (this.config.swing_arm) {
            // arm swing handled by item use / packet
        }

        if (imbue && this.config.cast_type == SpellCastType.USE_ITEM) {
            ctx.spellsCap.getCastingData()
                .imbueSpell(this, config.imbues);
        } else {
            attached.onCast(SpellCtx.onCast(caster, ctx.calcData));
        }
    }

    public final int getCooldownTicks(SpellCastContext ctx) {
        return (int) ctx.event.data.getNumber(EventData.COOLDOWN_TICKS).number;
    }

    public final int getCastTimeTicks(SpellCastContext ctx) {
        return (int) ctx.event.data.getNumber(EventData.CAST_TICKS).number;
    }

    @Override
    public String GUID() {
        return identifier;
    }

    public void spendResources(SpellCastContext ctx) {
        getManaCostCtx(ctx).Activate();
    }

    public SpendResourceEvent getManaCostCtx(SpellCastContext ctx) {
        float cost = this.getCalculatedManaCost(ctx);
        SpendResourceEvent event = new SpendResourceEvent(ctx.caster, ResourceType.mana, cost);
        event.calculateEffects();
        return event;
    }

    public final int getCalculatedManaCost(SpellCastContext ctx) {
        return (int) ctx.event.data.getNumber(EventData.MANA_COST).number;
    }

    public final List<Component> GetTooltipString(TooltipInfo info) {

        SpellCastContext ctx = new SpellCastContext(info.player, 0, this);

        List<Component> list = new ArrayList<>();

        list.add(locName().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

        TooltipUtils.addEmpty(list);

        if (true || Screen.hasShiftDown()) {

            SpellDesc.getTooltip(ctx.caster, this)
                .forEach(x -> list.add(Component.literal(x)));

        }

        TooltipUtils.addEmpty(list);

        list.add(Component.literal(ChatFormatting.BLUE + "Стоимость маны: " + getCalculatedManaCost(ctx)));
        if (config.usesCharges()) {
            list.add(Component.literal(ChatFormatting.YELLOW + "Макс. зарядов: " + config.charges));
            list.add(Component.literal(ChatFormatting.YELLOW + "Восстановление заряда: " + config.charge_regen / 20 + " с"));

        } else {
            list.add(Component.literal(ChatFormatting.YELLOW + "Перезарядка: " + (getCooldownTicks(ctx) / 20) + " с"));
        }

        int casttime = getCastTimeTicks(ctx);

        if (casttime == 0) {
            list.add(Component.literal(ChatFormatting.GREEN + "Время применения: Мгновенно"));

        } else {
            list.add(Component.literal(ChatFormatting.GREEN + "Время применения: " + casttime / 20 + " с"));

        }

        TooltipUtils.addEmpty(list);

        list.add(getConfig().castingWeapon.predicate.text);

        TooltipUtils.addEmpty(list);

        if (this.config.times_to_cast > 1) {
            TooltipUtils.addEmpty(list);
            list.add(Component.literal("Применяется " + config.times_to_cast + " раз за время поддержания.").withStyle(ChatFormatting.RED));

        }

        if (true || Screen.hasShiftDown()) {

            Set<ExileEffect> effect = new HashSet<>();

            if (ExileDB.ExileEffects()
                .isRegistered(effect_tip)) {
                effect.add(ExileDB.ExileEffects()
                    .get(effect_tip));
            }

            try {
                this.getAttached()
                    .getAllComponents()
                    .forEach(x -> {
                        x.acts.forEach(a -> {
                            if (a.has(MapField.EXILE_POTION_ID)) {
                                effect.add(a.getExileEffect());
                            }
                        });
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                effect.forEach(x -> list.addAll(x.GetTooltipString(info, ctx.calcData)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int currentlvl = Load.spells(info.player)
            .getLevelOf(GUID());
        int maxlvl = getMaxLevel();

        list.add(Component.literal("Уровень: " + currentlvl + "/" + maxlvl).withStyle(ChatFormatting.YELLOW));

        TooltipUtils.removeDoubleBlankLines(list);

        return list;

    }

    @Override
    public Class<Spell> getClassForSerialization() {
        return Spell.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.SPELL;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Spells;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".spell." + GUID();
    }

    public transient String locName;

    @Override
    public String locNameForLangFile() {
        return locName;
    }

    @Override
    public AutoLocGroup locDescGroup() {
        return AutoLocGroup.Spells;
    }

    @Override
    public String locDescLangFileGUID() {
        return "spell.desc." + GUID();
    }

    @Override
    public String locDescForLangFile() {
        return locDesc;
    }

    @Override
    public int getMaxLevel() {
        return max_lvl;
    }

    @Override
    public int getMaxLevelWithBonuses() {
        return getMaxLevel() + 4;
    }
}
