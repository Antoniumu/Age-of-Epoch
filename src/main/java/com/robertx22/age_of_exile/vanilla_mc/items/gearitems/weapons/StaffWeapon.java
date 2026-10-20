package com.robertx22.age_of_exile.vanilla_mc.items.gearitems.weapons;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.enumclasses.WeaponTypes;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.age_of_exile.vanilla_mc.packets.spells.TellServerToCastSpellPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StaffWeapon extends ModWeapon implements IAutoModel {
    VanillaMaterial mat;

    public StaffWeapon(VanillaMaterial mat) {
        super(mat.toolmat, new Properties().durability(mat.toolmat.getUses()), WeaponTypes.staff);
        this.mat = mat;

    }

    @Override
    public void generateModel(ItemModelManager manager) {
        manager.handheld(this);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        try {
            if (!world.isClientSide()) {

                GearItemData gear = Gear.Load(stack);

                if (gear != null && gear.hasSpell()) {
                    Spell spell = gear.getSpell();

                    if (TellServerToCastSpellPacket.tryCastSpell(player, spell)) {
                        return InteractionResult.SUCCESS;
                    }

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return InteractionResult.PASS;

    }
}
