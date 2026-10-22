package com.robertx22.orbs_of_crafting.register.reqs.vanilla;

import com.google.gson.JsonObject;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.ItemReqSers;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class VanillaRequirement extends ItemRequirement {
    transient String desc;
    public String itemTagId = "";
    public JsonObject vanillaCondition = new JsonObject();

    public VanillaRequirement() {
        super(ItemReqSers.VANILLA_CONDITION, "");
    }

    public VanillaRequirement(String id, String desc, TagKey<Item> tag) {
        super(ItemReqSers.VANILLA_CONDITION, id);
        this.desc = desc;
        this.itemTagId = tag.identifier().toString();
    }

    @Override
    public Class<?> getClassForSerialization() {
        return VanillaRequirement.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(Ref.MODID).desc(ExileTranslation.registry(this, desc == null ? "" : desc));
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        if (itemTagId != null && !itemTagId.isEmpty()) {
            return obj.stack.is(TagKey.create(Registries.ITEM, Identifier.parse(itemTagId)));
        }
        return true;
    }
}
