package com.robertx22.age_of_exile.a_libraries.trinkets;

/**
 * Trinket slots used by Age of Epoch, mapped to Trinkets Updated slot ids.
 * Replaces the old Curios slot names ("ring", "necklace").
 */
public enum TrinketSlots {

    NECKLACE("chest/necklace", "necklace"),
    RING("hand/ring", "ring");

    public final String trinketId;
    public final String name;

    TrinketSlots(String trinketId, String name) {
        this.trinketId = trinketId;
        this.name = name;
    }

    public static String trinketId(String name) {
        for (TrinketSlots slot : values()) {
            if (slot.name.equals(name)) {
                return slot.trinketId;
            }
        }
        return null;
    }

    public static String name(String trinketId) {
        for (TrinketSlots slot : values()) {
            if (slot.trinketId.equals(trinketId)) {
                return slot.name;
            }
        }
        return null;
    }

}
