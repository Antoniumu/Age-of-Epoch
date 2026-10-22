package top.theillusivec4.curios.api.type.capability;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ICuriosItemHandler {
    private final TrinketAttachment attachment;
    private final LivingEntity owner;

    public ICuriosItemHandler(TrinketAttachment attachment) {
        this(attachment, null);
    }

    public ICuriosItemHandler(TrinketAttachment attachment, LivingEntity owner) {
        this.attachment = attachment;
        this.owner = owner;
    }

    public static String mapSlotToTrinketId(String slot) {
        return switch (slot) {
            case "ring" -> "hand/ring";
            case "necklace" -> "chest/necklace";
            case "omen" -> "chest/omen";
            case "backpack", "back" -> "chest/back";
            default -> slot.contains("/") ? slot : "chest/" + slot;
        };
    }

    public Optional<ICurioStacksHandler> getStacksHandler(String slot) {
        if (attachment == null || slot == null) return Optional.empty();
        TrinketInventory inv = attachment.getInventory(mapSlotToTrinketId(slot));
        return inv == null ? Optional.empty() : Optional.of(new ICurioStacksHandler(inv));
    }

    public Map<String, ICurioStacksHandler> getCurios() {
        Map<String, ICurioStacksHandler> map = new LinkedHashMap<>();
        for (String key : new String[]{"ring", "necklace", "omen", "backpack"}) {
            getStacksHandler(key).ifPresent(h -> map.put(key, h));
        }
        return map;
    }

    public List<SlotResult> findCurios(String id) {
        List<SlotResult> out = new ArrayList<>();
        getStacksHandler(id).ifPresent(handler -> {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack st = stacks.getStackInSlot(i);
                if (!st.isEmpty()) {
                    out.add(new SlotResult(new SlotContext(id, owner, i, false, true), st));
                }
            }
        });
        return out;
    }
}
