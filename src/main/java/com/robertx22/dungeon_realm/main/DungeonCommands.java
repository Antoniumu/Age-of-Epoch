package com.robertx22.dungeon_realm.main;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.dungeon_realm.item.relic.RelicGenerator;
import com.robertx22.library_of_exile.command_wrapper.CommandBuilder;
import com.robertx22.library_of_exile.command_wrapper.PermWrapper;
import com.robertx22.library_of_exile.command_wrapper.PlayerWrapper;
import com.robertx22.library_of_exile.command_wrapper.RegistryWrapper;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.database.relic.relic_rarity.RelicRarity;
import com.robertx22.library_of_exile.database.relic.relic_type.RelicType;
import com.robertx22.library_of_exile.utils.PlayerUtil;

import java.util.Optional;

public class DungeonCommands {


    public static void init(CommandDispatcher d) {
       

        CommandBuilder.of(DungeonMain.MODID, d, x -> {
            PlayerWrapper PLAYER = new PlayerWrapper();
            var RARITY = new RegistryWrapper<RelicRarity>(LibDatabase.RELIC_RARITY);
            var TYPE = new RegistryWrapper<RelicType>(LibDatabase.RELIC_TYPE);

            x.addLiteral("give", PermWrapper.OP);
            x.addLiteral("relic", PermWrapper.OP);

            x.addArg(PLAYER);
            x.addArg(RARITY);
            x.addArg(TYPE);

            x.action(e -> {
                var p = PLAYER.get(e);
                var rar = RARITY.get(e);
                var type = TYPE.get(e);

                var set = new RelicGenerator.Settings();

                set.type = Optional.of(LibDatabase.RelicTypes().get(type));
                set.rar = Optional.of(LibDatabase.RelicRarities().get(rar));

                var stack = RelicGenerator.randomRelicItem(Optional.of(p), set);
                PlayerUtil.giveItem(stack, p);
            });

        }, "");
    }


}
