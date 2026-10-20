package com.robertx22.age_of_exile.uncommon.testing.tests;

import com.robertx22.age_of_exile.uncommon.testing.CommandTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class GivePlayerCapNbt extends CommandTest {

    @Override
    public void run(ServerPlayer player) {

        CompoundTag tag = new CompoundTag();
        System.out.print(tag.toString());
    }

    @Override
    public String GUID() {
        return "print_player_nbt";
    }
}
