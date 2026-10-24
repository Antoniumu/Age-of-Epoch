package com.robertx22.age_of_exile.database.data.gems;

import com.robertx22.age_of_exile.database.data.BaseRuneGem;
import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;
import net.minecraft.ChatFormatting;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
public class Gem extends BaseRuneGem implements IAutoGson<Gem>, JsonExileRegistry<Gem> {

    public static Gem SERIALIZER = new Gem();

    public String gem_type = "";
    public String text_format = "";

    public int perc_upgrade_chance = 0;

    /** Доля максимального уровня, с которого ранг может выпадать (0..1). */
    public float level_to_drop = 0;

    @Override
    public int getReqLevelToDrop() {
        if (Float.isFinite(level_to_drop) && level_to_drop > 0) {
            int maxLevel = 50;
            try {
                GameBalanceConfig config = GameBalanceConfig.get();
                if (config != null && config.MAX_LEVEL > 0) {
                    maxLevel = config.MAX_LEVEL;
                }
            } catch (Exception ignored) {
                // При отсутствии datapack сохраняем безопасный дефолт 50.
            }
            return Math.max(1, (int) Math.ceil(maxLevel * level_to_drop));
        }
        // Ранги из старых datapack-ов без level_to_drop продолжают работать по tier.
        return super.getReqLevelToDrop();
    }

    public ChatFormatting getFormat() {
        try {
            return ChatFormatting.valueOf(text_format);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return ChatFormatting.GRAY;
    }

    public Gem getHigherTierGem() {
        List<Gem> list = ExileDB.Gems()
            .getFiltered(x -> x.tier == tier + 1 && x.gem_type.equals(gem_type));

        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public boolean hasHigherTierGem() {
        return getHigherTierGem() != null;
    }

    public Item getItem() {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(item_id));
    }

    @Override
    public Class<Gem> getClassForSerialization() {
        return Gem.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.GEM;
    }

}
