package com.robertx22.the_harvest.main;

import com.robertx22.library_of_exile.command_wrapper.CommandBuilder;
import com.robertx22.library_of_exile.command_wrapper.PermWrapper;
import com.robertx22.library_of_exile.main.ApiForgeEvents;
import com.robertx22.the_harvest.structure.HarvestMapCap;
import com.robertx22.the_harvest.structure.HarvestWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.robertx22.forge_compat.event.RegisterCommandsEvent;

public class HarvestCommands {


    public static void init() {
        ApiForgeEvents.registerForgeEvent(RegisterCommandsEvent.class, event -> {
            var dis = event.getDispatcher();

            CommandBuilder.of(HarvestMain.MODID, dis, x -> {

                x.addLiteral("wipe_world_data", PermWrapper.OP);

                x.action(e -> {
                    var world = e.getSource().getServer().overworld();
                    HarvestMapCap.get(world).data = new HarvestWorldData();
                    e.getSource().getPlayer().sendSystemMessage(Component.literal(
                            "Harvest world data wiped, you should only do this when wiping the dimension's folder too! The dimension folder is in: savefolder\\dimensions\\the_harvest").withStyle(ChatFormatting.GREEN));
                });

            }, "");
        });
    }


}
