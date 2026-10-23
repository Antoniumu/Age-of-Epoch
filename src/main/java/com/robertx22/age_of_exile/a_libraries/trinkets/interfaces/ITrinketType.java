package com.robertx22.age_of_exile.a_libraries.trinkets.interfaces;

public interface ITrinketType {

    String trinketTypeName();

    default boolean rightClickEquip() {
        return true;
    }

}
