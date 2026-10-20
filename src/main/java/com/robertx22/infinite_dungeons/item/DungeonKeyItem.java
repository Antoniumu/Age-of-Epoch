package com.robertx22.infinite_dungeons.item;

import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.group.DungeonGroup;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.library_of_exile.registry.FilterListWrap;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
public class DungeonKeyItem extends Item {

    public DungeonKeyItem() {
        super(new Properties().tab(MainID.ITEM_GROUP));

    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity en, int num, boolean bool) {
        try {
            if (en.tickCount % 20 == 0) {
                if (!StackNBT.has(stack)) {
                    KeyData data = new KeyData();
                    setData(stack, data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public static DungeonGroup getDungeonGroup(Item key) {
        try {
            FilterListWrap<DungeonGroup> wrap = DungeonsDB.Groups()
                .getFilterWrapped(x -> x.getKeyItem() == key);

            if (wrap.list.isEmpty()) {
                return null;
            } else {
                return wrap.random();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setData(ItemStack stack, KeyData data) {
        LoadSave.Save(data, StackNBT.getOrCreate(stack), "dungeon");

    }

    public static KeyData getData(ItemStack stack) {
        return LoadSave.Load(KeyData.class, new KeyData(), StackNBT.getOrCreate(stack), "dungeon");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {

        try {
            list.add(MainID.ofTranslation("key_drop"));

        } catch (Exception e) {
            // e.printStackTrace();
        }

    }

}
