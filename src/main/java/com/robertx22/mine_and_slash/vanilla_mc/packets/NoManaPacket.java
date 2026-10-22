package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.event_hooks.ontick.OnClientTick;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public class NoManaPacket extends MyPacket<NoManaPacket> {

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "nomana");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (ClientConfigs.getConfig().SHOW_LOW_ENERGY_MANA_WARNING.get()) {
            if (OnClientTick.canSoundNoMana()) {
                OnClientTick.setNoManaSoundCooldown();
                Player player = ctx.getPlayer();
                player.playSound(com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.REDSTONE_TORCH_BURNOUT), 0.5F, 0);
            }
        }
    }

    @Override
    public MyPacket<NoManaPacket> newInstance() {
        return new NoManaPacket();
    }

    public NoManaPacket() {
    }

}