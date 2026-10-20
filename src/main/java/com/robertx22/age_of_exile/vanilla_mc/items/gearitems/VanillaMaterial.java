package com.robertx22.age_of_exile.vanilla_mc.items.gearitems;

import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Items;
import net.minecraft.tags.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;

public enum VanillaMaterial {

    WOOD("wood", new ItemOrTag(ItemTags.PLANKS), ArmorMaterial.LEATHER, Tier.WOOD),
    IRON("iron", new ItemOrTag(Items.IRON_INGOT), ArmorMaterial.IRON, Tier.IRON),
    GOLD("gold", new ItemOrTag(Items.GOLD_INGOT), ArmorMaterial.GOLD, Tier.GOLD),
    DIAMOND("diamond", new ItemOrTag(Items.DIAMOND), ArmorMaterial.DIAMOND, Tier.DIAMOND);

    public String id;
    public ItemOrTag mat;
    public ArmorMaterial armormat;
    public Tier toolmat;

    VanillaMaterial(String id, ItemOrTag mat, ArmorMaterial armormat, Tier toolmat) {
        this.id = id;
        this.mat = mat;
        this.armormat = armormat;
        this.toolmat = toolmat;
    }

    public static class ItemOrTag {
        public Item item;
        public Tag.INamedTag<Item> tag;

        public ItemOrTag(Item item) {
            this.item = item;
        }

        public ItemOrTag(Tag.INamedTag<Item> tag) {
            this.tag = tag;
        }
    }
}
