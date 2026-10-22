package com.robertx22.ancient_obelisks.database;

import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryContainer;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.SyncTime;
import com.robertx22.library_of_exile.registry.register_info.SeriazableRegistration;
import net.minecraft.data.CachedOutput;

public class ObeliskDatabase {

    public static SeriazableRegistration SERIALIZABLE_INFO = new SeriazableRegistration(ObelisksMain.MODID);

    public static ExileRegistryType OBELISK_TYPE = ExileRegistryType.register(ObelisksMain.MODID, "obelisk", 0, Obelisk.SERIALIZER, SyncTime.ON_LOGIN);

    public static void registerObjects() {

        String structureprefix = ObelisksMain.MODID + ":obelisk/";

        new Obelisk(new SimplePrebuiltMapData(1, structureprefix + "stone"), 1000, "stone").addToSerializables(SERIALIZABLE_INFO);

      
        // might not need super big builder classes if I won't have that many affixes?
        //    new AttributeObeliskAffix(ExileAffix.Affects.MOB, "speedy", 1000, Attributes.MOVEMENT_SPEED, "a250d6f0-c4b3-4a5d-854c-d259a671237a", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 1.25).addToSerializables(SERIALIZABLE_INFO);
        //    new AttributeObeliskAffix(ExileAffix.Affects.MOB, "unmoving", 1000, Attributes.KNOCKBACK_RESISTANCE, "b250d6f0-c4b3-4a5d-854c-d259a671237a", AttributeModifier.Operation.ADD_VALUE, 1).addToSerializables(SERIALIZABLE_INFO);
        //    new AttributeObeliskAffix(ExileAffix.Affects.PLAYER, "slow", 1000, Attributes.MOVEMENT_SPEED, "c250d6f0-c4b3-4a5d-854c-d259a671237a", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 0.75F).addToSerializables(SERIALIZABLE_INFO);

    }

    public static ExileRegistryContainer<Obelisk> getObelisks() {
        return (ExileRegistryContainer<Obelisk>) Database.getRegistry(OBELISK_TYPE);
    }


    public static void initRegistries() {

        Database.addRegistry(new ExileRegistryContainer<>(OBELISK_TYPE, "stone").setIsDatapack());

    }

    public static void generateJsons() {

        try {

            new ObeliskDataGen().run(CachedOutput.NO_CACHE);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
