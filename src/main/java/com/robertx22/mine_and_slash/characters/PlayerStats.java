package com.robertx22.mine_and_slash.characters;

import com.robertx22.mine_and_slash.database.data.stats.types.resources.blood.Blood;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.energy.Energy;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield.MagicShield;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.mana.Mana;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import net.minecraft.stats.Stats;
import com.robertx22.forge_compat.eventbus.api.SubscribeEvent;
import com.robertx22.forge_compat.fml.ModLoadingContext;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLCommonSetupEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStats {
    public static final Identifier LEVELS_GAINED = Identifier.fromNamespaceAndPath(SlashRef.MODID, "levels_gained");
    public static final HashMap<String, Identifier> REGISTERED_STATS = new HashMap<>();

    private static class Registrations {
        public final List<Identifier> customStats = new ArrayList<>();

        @SubscribeEvent
        public void commonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> customStats.forEach(it -> {
                Registry.register(BuiltInRegistries.CUSTOM_STAT, it.getPath(), it);
                Stats.CUSTOM.get(it);
            }));
        }
    }

    private static final Map<String, Registrations> registrations = new ConcurrentHashMap<>();

    public static void registerCustomStat(Identifier identifier) {
        getActiveRegistrations().customStats.add(identifier);
    }

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().register(getActiveRegistrations());
    }

    private static Registrations getActiveRegistrations() {
        return registrations.computeIfAbsent(ModLoadingContext.get().getActiveNamespace(), it -> new Registrations());
    }

    public static void addReg(String id) {
        REGISTERED_STATS.put(id, Identifier.fromNamespaceAndPath(SlashRef.MODID, id));
    }

    public static void initialize() {
        addReg(Health.getInstance().GUID());
        addReg(Mana.getInstance().GUID());
        addReg(Blood.getInstance().GUID());
        addReg(Energy.getInstance().GUID());
        addReg(MagicShield.getInstance().GUID());
        /*
        addReg(DatapackStats.DEX.GUID());
        addReg(DatapackStats.INT.GUID());
        addReg(DatapackStats.STR.GUID());
        addReg(DatapackStats.MOVE_SPEED.GUID());

         */

        registerCustomStat(LEVELS_GAINED);
        for (Identifier rl : REGISTERED_STATS.values())
            registerCustomStat(rl);
    }

}
