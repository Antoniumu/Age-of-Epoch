package top.theillusivec4.curios.api;

import com.robertx22.forge_compat.common.util.LazyOptional;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

public class CuriosApi {
    public static final String MODID = "curios";
    private static final ICuriosHelper HELPER = new ICuriosHelper() {};

    public static ICuriosHelper getCuriosHelper() {
        return HELPER;
    }

    public static LazyOptional<ICuriosItemHandler> getCuriosInventory(LivingEntity entity) {
        if (entity == null) return LazyOptional.empty();
        TrinketAttachment att = TrinketsApi.getAttachment(entity);
        return att == null ? LazyOptional.empty() : LazyOptional.of(() -> new ICuriosItemHandler(att, entity));
    }
}
