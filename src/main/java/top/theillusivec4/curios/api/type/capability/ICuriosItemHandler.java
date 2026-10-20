package top.theillusivec4.curios.api.type.capability;

import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Optional;

public interface ICuriosItemHandler {
    Optional<ICurioStacksHandler> getStacksHandler(String slot);
}
