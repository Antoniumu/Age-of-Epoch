package com.robertx22.age_of_exile.saveclasses.spells;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.data.spells.SpellCastType;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.MMORPG;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.effectdatas.EventBuilder;
import com.robertx22.age_of_exile.uncommon.effectdatas.SpendResourceEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.rework.RestoreType;
import com.robertx22.age_of_exile.uncommon.utilityclasses.OnScreenMessageUtils;
import com.robertx22.age_of_exile.vanilla_mc.packets.NoManaPacket;
import com.robertx22.library_of_exile.main.Packets;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import com.robertx22.forge_compat.network.ClientboundTitleCompatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

import java.util.List;

@Storable
public class SpellCastingData {

    @Store
    public int castingTicksLeft = 0;

    @Store
    public int castingTicksDone = 0;

    @Store
    public int lastSpellCastTimeInTicks = 0;

    @Store
    public String spellBeingCast = "";

    @Store
    public Boolean casting = false;

    @Store
    public ChargeData charges = new ChargeData();

    @Store
    public String imbued_spell = "";
    @Store
    public int imbued_spell_stacks = 0;

    public void imbueSpell(Spell spell, int amount) {
        this.imbued_spell = spell.GUID();
        this.imbued_spell_stacks = amount;
    }

    public void consumeImbuedSpell() {
        this.imbued_spell_stacks--;
        if (imbued_spell_stacks < 1) {
            imbued_spell = "";
        }
    }

    public boolean tryCastImbuedSpell(LivingEntity en) {
        if (imbued_spell_stacks <= 0 && isCasting()) {
            Spell castingSpell = getSpellBeingCast();
            if (castingSpell != null && castingSpell.config.cast_type == SpellCastType.USE_ITEM) {
                SpellCastContext ctx = new SpellCastContext(en, castingTicksDone, castingSpell);
                castingSpell.cast(ctx, true);
                onSpellCast(ctx);
                spellBeingCast = "";
            }
        }

        if (imbued_spell_stacks > 0) {

            Spell spell = ExileDB.Spells()
                .get(imbued_spell);

            if (spell != null) {
                spell.cast(new SpellCastContext(en, 0, spell), false);
                consumeImbuedSpell();
                if (en instanceof ServerPlayer) {
                    Load.spells(en)
                        .syncToClient((Player) en);
                }
                return true;
            }
        }
        return false;
    }

    public void cancelCast(LivingEntity entity) {
        cancelCast(entity, "cancelled");
    }

    public void cancelCast(LivingEntity entity, String reason) {
        try {
            if (isCasting()) {
                Spell spell = getSpellBeingCast();
                int refund = 0;
                if (spell != null) {
                    SpellCastContext ctx = new SpellCastContext(entity, 0, spell);
                    if (spell.getConfig().times_to_cast > 1) {
                        int cd = ctx.spell.getCooldownTicks(ctx);
                        Load.Unit(entity)
                            .getCooldowns()
                            .setOnCooldown(spell.GUID(), cd);
                    } else {
                        refund = spell.getCalculatedManaCost(ctx);
                        if (refund > 0) {
                            EventBuilder.ofRestore(entity, entity, ResourceType.mana, RestoreType.heal, refund)
                                .setSpell(spell)
                                .build()
                                .Activate();
                        }
                    }
                    com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastCancelled(
                            entity, spell.GUID(), refund, reason);
                }

                spellBeingCast = "";
                castingTicksLeft = 0;
                lastSpellCastTimeInTicks = 0;
                castingTicksDone = 0;
                this.casting = false;

                if (entity instanceof ServerPlayer) {
                    Load.Unit(entity)
                        .syncToClient((Player) entity);
                    Load.spells(entity)
                        .syncToClient((Player) entity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean isCasting() {
        return spellBeingCast != null && casting && ExileDB.Spells()
            .isRegistered(spellBeingCast);
    }

    transient static Spell lastSpell = null;

    public void onTimePass(LivingEntity entity, EntitySpellCap.ISpellsCap spells, int ticks) {

        try {

            if (isCasting()) {
                Spell spell = ExileDB.Spells()
                    .get(spellBeingCast);

                SpellCastContext ctx = new SpellCastContext(entity, castingTicksDone, spell);

                if (spell != null && spells != null && ExileDB.Spells()
                    .isRegistered(spell)) {
                    spell.onCastingTick(ctx);
                }

                tryCast(ctx);

                lastSpell = spell;

                castingTicksLeft--;
                castingTicksDone++;

                if (castingTicksLeft < 0) {
                    this.spellBeingCast = "";
                }
            } else {

                lastSpell = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            this.cancelCast(entity);
            // cancel when error, cus this is called on tick, so it doesn't crash servers when 1 spell fails
        }
    }

    public List<String> getSpellsOnCooldown(LivingEntity en) {
        return Load.Unit(en)
            .getCooldowns()
            .getAllSpellsOnCooldown();
    }

    public void setToCast(Spell spell, LivingEntity entity) {
        SpellCastContext ctx = new SpellCastContext(entity, 0, spell);

        this.spellBeingCast = spell.GUID();
        this.castingTicksLeft = ctx.spell.getCastTimeTicks(ctx);
        this.lastSpellCastTimeInTicks = this.castingTicksLeft;
        this.castingTicksDone = 0;
        this.casting = true;

        if (this.castingTicksLeft <= 0) {
            tryCast(ctx);
        }
    }

    public void tryCast(SpellCastContext ctx) {

        if (getSpellBeingCast() != null) {
            if (castingTicksLeft <= 0) {
                Spell spell = getSpellBeingCast();

                int timesToCast = ctx.spell.getConfig().times_to_cast;

                if (timesToCast == 1) {
                    spell.cast(ctx, true);
                }

                com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logSpellExecute(
                        ctx.caster, spell.GUID(), timesToCast);

                onSpellCast(ctx);
                spellBeingCast = "";

            }
        }

    }

    public Spell getSpellBeingCast() {

        if (!ExileDB.Spells()
            .isRegistered(spellBeingCast)) {
            return null;
        }

        return ExileDB.Spells()
            .get(spellBeingCast);
    }

    public boolean canCast(Spell spell, Player player) {

        if (player.level().isClientSide()) {
            return false;
        }

        if (isCasting()) {
            com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastAttempt(
                    player, spell != null ? spell.GUID() : "null", 0, false,
                    "Already casting another spell: " + spellBeingCast + " (ticksLeft=" + castingTicksLeft + ")");
            return false;
        }

        if (spell == null) {
            return false;
        }

        if (Load.Unit(player)
            .getCooldowns()
            .isOnCooldown(spell.GUID())) {
            com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastAttempt(
                    player, spell.GUID(), 0, false, "Spell is on cooldown");
            return false;
        }

        if (player.isCreative()) {
            return true;
        }

        if (spell.GUID()
            .contains("test")) {
            if (!MMORPG.RUN_DEV_TOOLS) {
                return false;
            }
        }

        if (spell.config.charges > 0) {
            if (!charges.hasCharge(spell.config.charge_name)) {
                com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastAttempt(
                        player, spell.GUID(), 0, false, "Not enough charges (" + spell.config.charge_name + ")");
                return false;
            }
        }

        SpellCastContext ctx = new SpellCastContext(player, 0, spell);

        LivingEntity caster = ctx.caster;

        if (caster instanceof Player == false) {
            return true;
        }

        if (((Player) caster).isCreative()) {
            return true;
        }

        if (!ServerContainer.get().BLACKLIST_SPELLS_IN_DIMENSIONS.isEmpty()) {
            Identifier id = ctx.caster.level().dimension().identifier();

            if (ServerContainer.get().BLACKLIST_SPELLS_IN_DIMENSIONS.stream()
                .anyMatch(x -> x.equals(id.toString()))) {
                com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastAttempt(
                        player, spell.GUID(), 0, false, "Spell blacklisted in dimension " + id);
                return false;
            }
        }

        EntityData data = Load.Unit(caster);

        if (data != null) {

            if (!spell.isAllowedInDimension(caster.level())) {
                if (caster instanceof Player) {
                    ((Player) caster).sendSystemMessage(Component.literal("Вы чувствуете чей-то взгляд. [Заклинание нельзя применить в этом измерении]"));
                }
                com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastAttempt(
                        player, spell.GUID(), 0, false, "Spell not allowed in dimension");
                return false;
            }

            SpendResourceEvent rctx = spell.getManaCostCtx(ctx);

            if (data.getResources()
                .hasEnough(rctx)) {

                if (!spell.getConfig().castingWeapon.predicate.predicate.test(caster)) {
                    if (caster instanceof ServerPlayer) {
                        OnScreenMessageUtils.sendMessage((ServerPlayer) caster, spell.getConfig().castingWeapon.predicate.text, ClientboundTitleCompatPacket.Type.ACTIONBAR);
                    }
                    com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastAttempt(
                            player, spell.GUID(), 0, false, "Weapon requirement not met: " + spell.getConfig().castingWeapon.predicate.text.getString());
                    return false;
                }

                GearItemData wep = Gear.Load(ctx.caster.getMainHandItem());

                if (wep != null && !wep.canPlayerWear(ctx.data)) {
                    if (ctx.caster instanceof ServerPlayer) {
                        OnScreenMessageUtils.sendMessage((ServerPlayer) ctx.caster, Component.literal("Требования оружия не выполнены"), ClientboundTitleCompatPacket.Type.ACTIONBAR);
                    }
                    com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastAttempt(
                            player, spell.GUID(), 0, false, "Player cannot wear equipped weapon stats");
                    return false;
                }

                return true;
            } else {
                if (caster instanceof ServerPlayer) {
                    Packets.sendToClient((Player) caster, new NoManaPacket());
                }
                com.robertx22.age_of_exile.uncommon.utilityclasses.SpellLogger.logCastAttempt(
                        player, spell.GUID(), (int) rctx.data.getNumber(), false, "Not enough mana");
            }
        }

        return false;

    }

    public void onSpellCast(SpellCastContext ctx) {

        int cd = ctx.spell.getCooldownTicks(ctx);

        ctx.data.getCooldowns()
            .setOnCooldown(ctx.spell.GUID(), cd);

        if (ctx.spell.config.charges > 0) {
            if (ctx.caster instanceof Player) {
                this.charges.spendCharge((Player) ctx.caster, ctx.spell.config.charge_name);
            }
        }

        if (ctx.caster instanceof Player) {
            Player p = (Player) ctx.caster;
            if (p.isCreative()) {
                if (cd > 20) {
                    ctx.data.getCooldowns()
                        .setOnCooldown(ctx.spell.GUID(), 20);

                }
            }
        }

        this.casting = false;

        if (ctx.caster instanceof ServerPlayer) {
            Load.Unit(ctx.caster)
                .syncToClient((Player) ctx.caster);
            Load.spells(ctx.caster)
                .syncToClient((Player) ctx.caster);
        }
    }

}
