package com.robertx22.divine_missions.data_gen.adders;

import com.robertx22.divine_missions.data_gen.builders.GodBuilder;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.ChatFormatting;

import java.util.Arrays;
import java.util.List;

public class GodsAdder implements ExileRegistryInit {

    public static String OCEAN = "ocean";
    public static String FORGE = "forge";
    public static String WISDOM = "wisdom";
    public static String HUNT = "hunt";
    public static String HARVEST = "harvest";

    public static List<String> ALL_GODS = Arrays.asList(OCEAN, FORGE, HUNT, WISDOM, HARVEST);

    @Override
    public void registerAll() {

        GodBuilder.of("ocean", ChatFormatting.AQUA);
        GodBuilder.of("forge", ChatFormatting.GRAY);
        GodBuilder.of("hunt", ChatFormatting.DARK_GREEN);
        GodBuilder.of("wisdom", ChatFormatting.LIGHT_PURPLE);
        GodBuilder.of("harvest", ChatFormatting.YELLOW);

    }

}
