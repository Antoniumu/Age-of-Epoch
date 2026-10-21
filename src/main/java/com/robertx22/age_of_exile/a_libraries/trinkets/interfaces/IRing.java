package com.robertx22.age_of_exile.a_libraries.trinkets.interfaces;

import com.robertx22.age_of_exile.a_libraries.trinkets.TrinketSlots;

public interface IRing extends ITrinketType {

    @Override
    public default String trinketTypeName() {
        return TrinketSlots.RING.name;
    }

}
