package com.robertx22.age_of_exile.vanilla_mc.packets;

import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class AllocateSpellPacket extends MyPacket<AllocateSpellPacket> {

    public String spellid;
    public String schoolid;
    AllocateSpellPacket.ACTION action;

    public enum ACTION {
        ALLOCATE, REMOVE
    }

    public AllocateSpellPacket() {

    }

    public AllocateSpellPacket(SpellSchool school, Spell spell, ACTION action) {
        this.spellid = spell.GUID();
        this.schoolid = school.GUID();
        this.action = action;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "spell_alloc");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        spellid = tag.readUtf(100);
        schoolid = tag.readUtf(100);
        action = tag.readEnum(AllocateSpellPacket.ACTION.class);

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeUtf(spellid, 100);
        tag.writeUtf(schoolid, 100);
        tag.writeEnum(action);

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player player = ctx.getPlayer();
        EntitySpellCap.ISpellsCap spells = Load.spells(player);

        Spell spell = ExileDB.Spells()
            .get(this.spellid);
        SpellSchool school = ExileDB.SpellSchools()
            .get(this.schoolid);

        if (this.action == ACTION.REMOVE) {
            boolean inSim = WorldUtils.isSimulationWorld(player.level());
            RPGPlayerData rpgData = Load.playerRPGData(player);
            if (inSim || rpgData.talents.reset_points > 0) {
                if (spells.getSpellsData().unlearnNode(this.spellid, school)) {
                    if (!inSim && rpgData.talents.reset_points > 0) {
                        rpgData.talents.reset_points--;
                        rpgData.syncToClient(player);
                    }
                    Load.Unit(player).setEquipsChanged(true);
                }
            }
        } else if (spell != null && school != null && spells.canLearn(school, spell)) {
            spells.getSpellsData()
                .learnSpell(spell, school);
            Load.Unit(player).setEquipsChanged(true);
        }

        spells.syncToClient(player);

    }

    @Override
    public MyPacket<AllocateSpellPacket> newInstance() {
        return new AllocateSpellPacket();
    }
}

