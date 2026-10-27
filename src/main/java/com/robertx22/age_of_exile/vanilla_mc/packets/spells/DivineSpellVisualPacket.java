package com.robertx22.age_of_exile.vanilla_mc.packets.spells;

import com.robertx22.age_of_exile.database.data.spells.visuals.DivineSpellVisuals;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Sends a visual-only cast cue; it never replays gameplay actions on clients. */
public class DivineSpellVisualPacket extends MyPacket<DivineSpellVisualPacket> {

    private String spellId = "";
    private int entityId;

    public DivineSpellVisualPacket() {
    }

    public DivineSpellVisualPacket(LivingEntity caster, String spellId) {
        this.spellId = spellId;
        this.entityId = caster.getId();
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "divine_spell_visual");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        spellId = tag.readUtf(64);
        entityId = tag.readInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeUtf(spellId, 64);
        tag.writeInt(entityId);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (!DivineSpellVisuals.hasVisual(spellId)) {
            return;
        }

        Entity entity = ctx.getPlayer().level().getEntity(entityId);
        if (entity instanceof LivingEntity caster) {
            DivineSpellVisuals.play(spellId, caster);
        }
    }

    @Override
    public MyPacket<DivineSpellVisualPacket> newInstance() {
        return new DivineSpellVisualPacket();
    }
}
