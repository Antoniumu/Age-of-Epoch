package com.robertx22.age_of_exile.database.data.gear_slots;

import com.robertx22.age_of_exile.a_libraries.curios.RefCurio;
import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.data.gear_types.bases.SlotFamily;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.weapons.StaffWeapon;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.forge_compat.common.ArmorItem;
import com.robertx22.forge_compat.common.AxeItem;
import com.robertx22.forge_compat.common.SwordItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;

public class GearSlot implements JsonExileRegistry<GearSlot>, IAutoGson<GearSlot>, IAutoLocName {

    public static GearSlot SERIALIZER = new GearSlot("", "", SlotFamily.NONE, 1, -1, 0);
    private static HashMap<String, HashMap<Item, Boolean>> CACHED_GEAR_SLOTS = new HashMap<>();
    static HashMap<Item, GearSlot> CACHED = new HashMap<>();

    public String id;
    public int weight;
    public int energy_cost;
    public int model_num = -1;
    public transient String locname = "";
    public SlotFamily fam = SlotFamily.Armor;

    public GearSlot(String id, String name, SlotFamily fam, int energy_cost, int modelnnum, int weight) {
        this.id = id;
        this.fam = fam;
        this.energy_cost = energy_cost;
        this.locname = name;
        this.model_num = modelnnum;
        this.weight = weight;
    }

    public static GearSlot getSlotOf(Item item) {

        GearSlot cached = CACHED.get(item);
        if (cached != null) {
            return cached;
        }

        if (ServerContainer.get()
            .getCompatMap()
            .containsKey(item)) {
            GearSlot compatSlot = ServerContainer.get()
                .getCompatMap()
                .get(item);
            if (compatSlot != null) {
                CACHED.put(item, compatSlot);
                return compatSlot;
            }
        }

        for (GearSlot slot : ExileDB.GearSlots()
            .getList()) {
            if (isItemOfThisSlot(slot, item)) {
                CACHED.put(item, slot);
                return slot;
            }
        }

        return null;
    }

    // has to use ugly stuff like this cus datapacks.
    public static boolean isItemOfThisSlot(GearSlot slot, Item item) {
        if (item == Items.AIR) {
            return false;
        }

        if (slot == null) {
            return false;
        }
        String id = slot.GUID();

        if (id.isEmpty()) {
            return false;
        }

        if (!CACHED_GEAR_SLOTS.containsKey(id)) {
            CACHED_GEAR_SLOTS.put(id, new HashMap<>());
        }
        if (Boolean.TRUE.equals(CACHED_GEAR_SLOTS.get(id).get(item))) {
            return true;
        }

        boolean bool = false;

        try {

            if (ServerContainer.get()
                .getCompatMap()
                .containsKey(item)) {
                if (ServerContainer.get()
                    .getCompatMap()
                    .get(item)
                    .GUID()
                    .equals(slot.GUID())) {
                    bool = true;
                }
            }

            if (!bool) {
                String itemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
                ItemStack defaultStack = item.getDefaultInstance();

                if (item instanceof ArmorItem) {
                    EquipmentSlot eqslot = ((ArmorItem) item).getSlot();
                    if (eqslot == EquipmentSlot.CHEST && id.equals(GearSlots.CHEST)) {
                        bool = true;
                    } else if (eqslot == EquipmentSlot.LEGS && id.equals(GearSlots.PANTS)) {
                        bool = true;
                    } else if (eqslot == EquipmentSlot.HEAD && id.equals(GearSlots.HELMET)) {
                        bool = true;
                    } else if (eqslot == EquipmentSlot.FEET && id.equals(GearSlots.BOOTS)) {
                        bool = true;
                    }

                } else if (id.equals(GearSlots.HELMET) && (itemPath.endsWith("_helmet") || defaultStack.is(net.minecraft.tags.ItemTags.HEAD_ARMOR))) {
                    bool = true;
                } else if (id.equals(GearSlots.CHEST) && (itemPath.endsWith("_chestplate") || defaultStack.is(net.minecraft.tags.ItemTags.CHEST_ARMOR))) {
                    bool = true;
                } else if (id.equals(GearSlots.PANTS) && (itemPath.endsWith("_leggings") || defaultStack.is(net.minecraft.tags.ItemTags.LEG_ARMOR))) {
                    bool = true;
                } else if (id.equals(GearSlots.BOOTS) && (itemPath.endsWith("_boots") || defaultStack.is(net.minecraft.tags.ItemTags.FOOT_ARMOR))) {
                    bool = true;
                } else if (id.equals(GearSlots.SWORD)) {
                    bool = item instanceof SwordItem || itemPath.endsWith("_sword") || defaultStack.is(net.minecraft.tags.ItemTags.SWORDS);
                } else if (id.equals(GearSlots.BOW)) {
                    bool = item instanceof BowItem || itemPath.equals("bow") || itemPath.endsWith("_bow");
                } else if (id.equals(GearSlots.AXE)) {
                    bool = item instanceof AxeItem || itemPath.endsWith("_axe") || defaultStack.is(net.minecraft.tags.ItemTags.AXES);
                } else if (id.equals(GearSlots.SHIELD)) {
                    bool = item instanceof ShieldItem || itemPath.equals("shield") || itemPath.endsWith("_shield");
                } else if (id.equals(GearSlots.CROSBOW)) {
                    bool = item instanceof CrossbowItem || itemPath.equals("crossbow") || itemPath.endsWith("_crossbow");
                } else if (id.equals(GearSlots.SCEPTER) || id.equals(GearSlots.STAFF)) {
                    bool = item instanceof StaffWeapon;
                } else if (id.equals(GearSlots.NECKLACE)) {
                    bool = item instanceof com.robertx22.age_of_exile.vanilla_mc.items.gearitems.baubles.ItemNecklace
                        || itemPath.contains("necklace")
                        || CuriosApi.getCuriosHelper().getCurioTags(item).contains(RefCurio.NECKLACE);
                } else if (id.equals(GearSlots.RING)) {
                    bool = item instanceof com.robertx22.age_of_exile.vanilla_mc.items.gearitems.baubles.ItemRing
                        || itemPath.contains("ring")
                        || CuriosApi.getCuriosHelper().getCurioTags(item).contains(RefCurio.RING);
                }
            }

            if (bool) {
                CACHED_GEAR_SLOTS.get(id).put(item, true);
            }

            return bool;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;

    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.GEAR_SLOT;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Gear_Slots;
    }

    @Override
    public String locNameLangFileGUID() {
        return "mmorpg.gearslot." + id;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    @Override
    public Class<GearSlot> getClassForSerialization() {
        return GearSlot.class;
    }
}
