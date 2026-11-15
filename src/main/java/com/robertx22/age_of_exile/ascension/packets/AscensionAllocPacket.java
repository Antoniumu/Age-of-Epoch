package com.robertx22.age_of_exile.ascension.packets;

import com.robertx22.age_of_exile.ascension.AscensionLogic;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * C2S: вложить очко Вознесения в узел линейной цепочки.
 */
public class AscensionAllocPacket extends MyPacket<AscensionAllocPacket> {

    String spellId;

    public AscensionAllocPacket() {
    }

    public AscensionAllocPacket(String spellId) {
        this.spellId = spellId;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "ascension_alloc");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        this.spellId = tag.readUtf(100);
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeUtf(spellId == null ? "" : spellId, 100);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (!ExileDB.Spells()
            .isRegistered(spellId)) {
            return;
        }

        String err = AscensionLogic.canAllocate(ctx.getPlayer(), spellId);

        if (!err.isEmpty()) {
            ctx.getPlayer()
                .sendSystemMessage(Component.literal(switch (err) {
                    case "low_level" -> "§cУ вас ещё нет 50-го уровня.";
                    case "no_class" -> "§cСначала выберите Вознесённый класс.";
                    case "wrong_class" -> "§cЭто умение не принадлежит вашему пути.";
                    case "max_rank" -> "§eЭтот узел уже достиг предела.";
                    case "chain_locked" -> "§cВложите очки в предыдущие умения пути.";
                    default -> "§cНет свободных очков Вознесения.";
                }));
            return;
        }

        if (AscensionLogic.allocate(ctx.getPlayer(), spellId)) {
            ctx.getPlayer()
                .sendSystemMessage(Component.literal("§bПрилив усилил «" + ExileDB.Spells()
                    .get(spellId)
                    .locName()
                    .getString() + "»."));
        }
    }

    @Override
    public MyPacket<AscensionAllocPacket> newInstance() {
        return new AscensionAllocPacket();
    }
}
