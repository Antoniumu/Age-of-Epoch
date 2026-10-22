package top.theillusivec4.curios.api.type.util;

import com.robertx22.forge_compat.common.util.LazyOptional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public interface ICuriosHelper {
    default LazyOptional<ICuriosItemHandler> getCuriosHandler(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity);
    }

    default Optional<ItemStack> findFirstCurio(LivingEntity entity, Item item) {
        return Optional.empty();
    }

    default Set<String> getCurioTags(Item item) {
        Set<String> out = new HashSet<>();
        if (item == null) return out;
        String cls = item.getClass().getSimpleName().toLowerCase();
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase();
        if (cls.contains("ring") || id.contains("ring")) out.add("ring");
        if (cls.contains("necklace") || id.contains("necklace") || id.contains("amulet")) out.add("necklace");
        if (cls.contains("omen") || id.contains("omen")) out.add("omen");
        return out;
    }
}
