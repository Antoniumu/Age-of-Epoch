package com.robertx22.orbs_of_crafting.register.reqs.vanilla;

import com.google.gson.JsonObject;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import com.robertx22.orbs_of_crafting.register.mods.base.VanillaItemModSers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class VanillaItemMod extends ItemModification {
    transient String desc;
    public int enchantLevels = 0;
    public JsonObject vanillaFunction = new JsonObject();

    public VanillaItemMod() {
        super(VanillaItemModSers.VANILLA, "");
    }

    public VanillaItemMod(String id, String desc, int levels) {
        super(VanillaItemModSers.VANILLA, id);
        this.desc = desc;
        this.enchantLevels = levels;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return VanillaItemMod.class;
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.NEUTRAL;
    }

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {
    }

    @Override
    public void applyMod(Player p, StackHolder stack, ItemModificationResult r) {
        if (enchantLevels > 0 && p != null && p.level() != null) {
            var list = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList();
            if (!list.isEmpty()) {
                var picked = list.get(p.getRandom().nextInt(list.size()));
                int lvl = Math.max(1, Math.min(3, enchantLevels / 10));
                EnchantmentHelper.updateEnchantments(stack.stack, mut -> mut.set(picked, Math.max(lvl, mut.getLevel(picked))));
            }
        }
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(Ref.MODID).desc(ExileTranslation.registry(this, desc == null ? "" : desc));
    }
}
